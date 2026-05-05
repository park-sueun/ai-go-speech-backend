package com.aigo.speech.answer.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.prompt.PromptTemplate;
import com.aigo.speech.ai.service.AiService;
import com.aigo.speech.answer.dto.AnswerAnalysisResponse;
import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.answer.exception.AnswerAnalysisNotFoundException;
import com.aigo.speech.answer.repository.AnswerAnalysisRepository;
import com.aigo.speech.answer.repository.InterviewAnswerRepository;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.repository.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerAnalysisService {

	// AI 응답 내 unescaped 제어 문자(LF 등)를 허용하는 전용 mapper
	private static final JsonMapper LENIENT_MAPPER = JsonMapper.builder()
		.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
		.build();

	private final InterviewAnswerRepository answerRepository;
	private final AnswerAnalysisRepository analysisRepository;
	private final InterviewSessionRepository sessionRepository;
	private final InterviewReportRepository reportRepository;
	private final InterviewQuestionRepository questionRepository;
	private final AiService aiService;
	private final SseEmitterService sseEmitterService;

	// self-injection: generateReportAsync 내에서 @Transactional 메서드를 프록시 경유 호출하기 위함
	@Autowired
	@Lazy
	private AnswerAnalysisService self;

	// ── createAnalysis 3단계 분리 ──────────────────────────────────────────────

	/**
	 * 1단계: DB 읽기 전용 — 커넥션을 즉시 반납.
	 * silenceCount/totalSilenceDuration은 이벤트에서 전달받은 값을 함께 보관.
	 */
	@Transactional(readOnly = true)
	public AnswerData loadAnswerData (Long answerId, Integer silenceCount, Integer totalSilenceDuration) {
		InterviewAnswer answer = answerRepository.findById(answerId)
			.orElseThrow(() -> new IllegalStateException("답변을 찾을 수 없습니다: " + answerId));
		InterviewQuestion question = answer.getQuestion();
		InterviewSession session = question.getSession();
		Map<String, Integer> fillerWordCounts = countFillerWords(answer.getFillerWords());
		return new AnswerData(answer, question, session, fillerWordCounts, silenceCount, totalSilenceDuration);
	}

	/**
	 * 2단계: AI 호출 — 트랜잭션 없음. DB 커넥션 비점유 상태에서 외부 I/O 수행.
	 */
	public AiAnalysisResult performAiAnalysis (AnswerData data) {
		try {
			AiPromptRequest request = AiPromptRequest.of(
				PromptTemplate.ANSWER_ANALYSIS_V1.getContent(),
				Map.of(
					"question", orEmpty(data.question().getContent()),
					"transcript", orEmpty(data.answer().getTranscript()),
					"silenceDurationSec", String.valueOf(data.totalSilenceDuration() / 1000),
					"silenceCount", String.valueOf(data.silenceCount()),
					"fillerEum", String.valueOf(data.fillerWordCounts().getOrDefault("음", 0)),
					"fillerEo", String.valueOf(data.fillerWordCounts().getOrDefault("어", 0)),
					"fillerGeu", String.valueOf(data.fillerWordCounts().getOrDefault("그", 0))
				)
			);
			AiResponse response = aiService.complete(request);
			Map<String, Object> parsed = LENIENT_MAPPER.readValue(
				response.content(), new TypeReference<Map<String, Object>>() {
				}
			);
			String aiReview = (String)parsed.get("aiReview");
			int logicScore = ((Number)parsed.get("logicScore")).intValue();
			return new AiAnalysisResult(aiReview, logicScore);
		} catch (Exception e) {
			log.error("[AnswerAnalysis] AI 분석 실패. answerId={}, error={}", data.answer().getId(), e.getMessage());
			return new AiAnalysisResult(null, 0);
		}
	}

	/**
	 * 3단계: DB 저장 — 짧은 쓰기 트랜잭션.
	 * 해당 세션의 마지막 분석이면 sessionId 반환, 아니면 null 반환.
	 */
	@Transactional
	public Long saveAnalysisResult (AnswerData data, AiAnalysisResult aiResult) {
		int silenceScore = calcSilenceScore(data.totalSilenceDuration(), data.silenceCount());
		int fillerScore = calcFillerScore(data.fillerWordCounts());
		int totalScore = (int)Math.round((silenceScore + fillerScore + aiResult.logicScore()) / 3.0);

		analysisRepository.save(
			new AnswerAnalysis(
				data.answer(), data.totalSilenceDuration(), data.silenceCount(),
				data.fillerWordCounts(), aiResult.aiReview(),
				aiResult.logicScore(), silenceScore, fillerScore, totalScore
			)
		);

		log.info("[AnswerAnalysis] 분석 저장 완료. answerId={}", data.answer().getId());

		long analysisCount = analysisRepository.countByAnswerQuestionSession(data.session());
		long totalQuestions = questionRepository.countBySession(data.session());

		return analysisCount >= totalQuestions ? data.session().getId() : null;
	}

	// ── generateReportAsync 분리 ───────────────────────────────────────────────

	/**
	 * 비동기 리포트 생성. @Transactional 없음 — self-injection으로 각 단계가 개별 트랜잭션 관리.
	 */
	@Async("answerExecutor")
	@Transactional(propagation = Propagation.NOT_SUPPORTED) // 오케스트레이션 메서드: 트랜잭션 없이 실행 (DB 작업은 하위 메서드에서 처리)
	public void generateReportAsync (Long sessionId) {
		SessionReportContext ctx = self.loadSessionReportContext(sessionId);
		if (ctx == null) {
			log.info("[SessionReport] 이미 처리 중이거나 완료된 세션. sessionId={}", sessionId);
			return;
		}

		UUID sessionUuid = ctx.sessionUuid();
		try {
			AiResponse response = aiService.complete(ctx.promptRequest());
			self.saveInterviewReport(sessionId, ctx, response);

			log.info("[SessionReport] 리포트 생성 완료. sessionId={}", sessionId);
			sseEmitterService.sendEvent(
				sessionUuid, "SESSION_ANALYSIS_DONE",
				Map.of("sessionUuid", sessionUuid.toString())
			);
			sseEmitterService.complete(sessionUuid);
		} catch (Exception e) {
			log.error("[SessionReport] 리포트 생성 실패. sessionId={}, error={}", sessionId, e.getMessage());
			self.markReportFailed(sessionId);
			sseEmitterService.sendEvent(sessionUuid, "ERROR", Map.of("message", "리포트 생성에 실패했습니다."));
			sseEmitterService.complete(sessionUuid);
		}
	}

	/**
	 * 리포트 생성을 위한 세션 데이터 로드. GENERATING 상태로 InterviewReport 선삽입.
	 * 이미 처리 중이거나 완료된 경우 null 반환.
	 */
	@Transactional
	public SessionReportContext loadSessionReportContext (Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new IllegalStateException("면접 세션을 찾을 수 없습니다: " + sessionId));

		if (reportRepository.existsBySessionAndStatusNot(session, InterviewReport.ReportStatus.FAILED)) {
			return null;
		}

		// GENERATING 상태 선삽입 — DB의 session_id UNIQUE 제약이 동시 중복 삽입 방지
		InterviewReport report = reportRepository.save(new InterviewReport(session));

		List<AnswerAnalysis> analyses = analysisRepository.findBySessionWithDetails(session);
		String analysesText = buildAnalysesText(analyses);

		AiPromptRequest promptRequest = AiPromptRequest.of(
			PromptTemplate.SESSION_REPORT_V1.getContent(),
			Map.of("analyses", analysesText)
		);

		int avgSilenceScore = avgScore(analyses, AnswerAnalysis::getSilenceScore);
		int avgFillerScore = avgScore(analyses, AnswerAnalysis::getFillerScore);
		int avgLogicScore = avgScore(analyses, AnswerAnalysis::getLogicScore);
		int avgTotalScore = avgScore(analyses, AnswerAnalysis::getTotalScore);

		return new SessionReportContext(
			session.getUuid(), report.getId(), promptRequest,
			avgSilenceScore, avgFillerScore, avgLogicScore, avgTotalScore
		);
	}

	/**
	 * AI 응답 수신 후 리포트를 COMPLETED 상태로 업데이트.
	 */
	@Transactional
	public void saveInterviewReport (Long sessionId, SessionReportContext ctx, AiResponse response) {
		InterviewReport report = reportRepository.findById(ctx.reportId())
			.orElseThrow(() -> new IllegalStateException("리포트를 찾을 수 없습니다. sessionId=" + sessionId));
		report.complete(
			response.content().trim(),
			ctx.avgSilenceScore(), ctx.avgFillerScore(), ctx.avgLogicScore(), ctx.avgTotalScore()
		);
	}

	/**
	 * AI 실패 시 리포트를 FAILED 상태로 업데이트.
	 */
	@Transactional
	public void markReportFailed (Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
		if (session == null)
			return;
		reportRepository.findBySession(session).ifPresent(InterviewReport::fail);
	}

	// ── 조회 API ───────────────────────────────────────────────────────────────

	public AnswerAnalysisResponse getByUuid (UUID analysisUuid, String userUuidStr) {
		AnswerAnalysis analysis = analysisRepository.findByUuid(analysisUuid)
			.orElseThrow(() -> new AnswerAnalysisNotFoundException("답변 분석을 찾을 수 없습니다."));

		InterviewSession session = analysis.getAnswer().getQuestion().getSession();
		if (!session.getUser().getUuid().equals(UUID.fromString(userUuidStr))) {
			throw new AnswerAnalysisNotFoundException("답변 분석을 찾을 수 없습니다.");
		}

		return AnswerAnalysisResponse.from(analysis);
	}

	public List<AnswerAnalysisResponse> getBySessionUuid (UUID sessionUuid, String userUuidStr) {
		InterviewSession session = sessionRepository.findByUuid(sessionUuid)
			.orElseThrow(() -> new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다."));

		if (!session.getUser().getUuid().equals(UUID.fromString(userUuidStr))) {
			throw new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다.");
		}

		return analysisRepository.findBySessionWithDetails(session)
			.stream()
			.map(AnswerAnalysisResponse::from)
			.toList();
	}

	// ── 내부 DTO ───────────────────────────────────────────────────────────────

	public record AnswerData(
		InterviewAnswer answer,
		InterviewQuestion question,
		InterviewSession session,
		Map<String, Integer> fillerWordCounts,
		Integer silenceCount,
		Integer totalSilenceDuration
	) {
	}

	public record AiAnalysisResult(String aiReview, int logicScore) {
	}

	public record SessionReportContext(
		UUID sessionUuid,
		Long reportId,
		AiPromptRequest promptRequest,
		int avgSilenceScore,
		int avgFillerScore,
		int avgLogicScore,
		int avgTotalScore
	) {
	}

	// ── 유틸 ───────────────────────────────────────────────────────────────────

	private Map<String, Integer> countFillerWords (List<String> fillerWords) {
		List<String> targets = List.of("음", "어", "그");
		if (fillerWords == null || fillerWords.isEmpty()) {
			return targets.stream().collect(Collectors.toMap(k -> k, k -> 0));
		}
		return targets.stream().collect(
			Collectors.toMap(k -> k, k -> (int)fillerWords.stream().filter(k::equals).count())
		);
	}

	private String buildAnalysesText (List<AnswerAnalysis> analyses) {
		StringBuilder sb = new StringBuilder();
		for (AnswerAnalysis aa : analyses) {
			InterviewQuestion q = aa.getAnswer().getQuestion();
			Map<String, Integer> fillers = aa.getFillerWords();
			sb.append("질문 ").append(q.getSequenceOrder()).append(": ").append(q.getContent()).append("\n");
			sb.append("답변: ").append(orEmpty(aa.getAnswer().getTranscript())).append("\n");
			sb.append("AI 분석: ").append(orEmpty(aa.getAiReview())).append("\n");
			sb.append("논리 점수: ").append(aa.getLogicScore()).append("점\n");
			sb.append("침묵 시간: ").append(aa.getTotalSilenceDuration() / 1000).append("초, ");
			sb.append("침묵 횟수: ").append(aa.getSilenceCount()).append("회\n");
			if (fillers != null) {
				sb.append("습관어: 음 ").append(fillers.getOrDefault("음", 0)).append("회, ");
				sb.append("어 ").append(fillers.getOrDefault("어", 0)).append("회, ");
				sb.append("그 ").append(fillers.getOrDefault("그", 0)).append("회\n");
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}

	private int calcSilenceScore (Integer totalSilenceDuration, Integer silenceCount) {
		int durationSec = totalSilenceDuration / 1000;
		int deduction = Math.max(0, durationSec - 5 * silenceCount);
		return Math.max(0, 100 - deduction);
	}

	private int calcFillerScore (Map<String, Integer> fillerWordCounts) {
		int totalFillers = fillerWordCounts.values().stream().mapToInt(Integer::intValue).sum();
		return Math.max(0, 100 - totalFillers);
	}

	private int avgScore (List<AnswerAnalysis> analyses, Function<AnswerAnalysis, Integer> getter) {
		return (int)Math.round(
			analyses.stream().mapToInt(aa -> getter.apply(aa) != null ? getter.apply(aa) : 0).average().orElse(0)
		);
	}

	private String orEmpty (String value) {
		return value != null ? value : "";
	}
}
