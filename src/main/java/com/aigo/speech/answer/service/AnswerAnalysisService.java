package com.aigo.speech.answer.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.aigo.speech.global.dto.TimePeriod;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
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

	private static final int TRANSCRIPT_MAX_LEN = 600;

	// AI 응답 내 unescaped 제어 문자(LF 등)를 허용하는 전용 mapper
	private static final JsonMapper LENIENT_MAPPER = JsonMapper.builder()
		.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
		.build();

	private final InterviewAnswerRepository answerRepository;
	private final AnswerAnalysisRepository analysisRepository;
	private final InterviewSessionRepository sessionRepository;
	private final InterviewReportRepository reportRepository;
	private final AiService aiService;
	private final SseEmitterService sseEmitterService;

	// self-injection: @Transactional 메서드를 프록시 경유 호출하기 위함
	@Autowired
	@Lazy
	private AnswerAnalysisService self;

	// ── 일괄 분석 + 리포트 생성 ────────────────────────────────────────────────

	/**
	 * 오케스트레이션 메서드 — 트랜잭션 없이 하위 단계가 각자 트랜잭션을 관리.
	 */
	@Async("answerExecutor")
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void generateAllAnalysesAndReport(Long sessionId) {
		AllAnalysisContext ctx = self.loadAllAnalysisContext(sessionId);
		if (ctx == null) {
			log.info("[CombinedAnalysis] 이미 처리 중이거나 완료된 세션. sessionId={}", sessionId);
			return;
		}

		UUID sessionUuid = ctx.sessionUuid();
		try {
			AiResponse response = aiService.complete(ctx.promptRequest());
			self.saveAllAnalysesAndReport(sessionId, ctx, response);

			log.info("[CombinedAnalysis] 분석 완료. sessionId={}", sessionId);
			sseEmitterService.sendEvent(
				sessionUuid, "SESSION_ANALYSIS_DONE",
				Map.of("sessionUuid", sessionUuid.toString())
			);
			sseEmitterService.complete(sessionUuid);
		} catch (Exception e) {
			log.error("[CombinedAnalysis] 실패. sessionId={}, error={}", sessionId, e.getMessage());
			self.markReportFailed(sessionId);
			sseEmitterService.sendEvent(sessionUuid, "ERROR", Map.of("message", "리포트 생성에 실패했습니다."));
			sseEmitterService.complete(sessionUuid);
		}
	}

	/**
	 * 1단계: DB 읽기 + GENERATING 리포트 선삽입. 이미 처리 중이면 null 반환.
	 */
	@Transactional
	public AllAnalysisContext loadAllAnalysisContext(Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new IllegalStateException("면접 세션을 찾을 수 없습니다: " + sessionId));

		if (reportRepository.existsBySessionAndStatusNot(session, InterviewReport.ReportStatus.FAILED)) {
			return null;
		}

		// GENERATING 상태 선삽입 — session_id UNIQUE 제약으로 동시 중복 삽입 방지
		InterviewReport report = reportRepository.save(new InterviewReport(session));

		List<InterviewAnswer> answers = answerRepository.findBySessionOrderBySequenceOrder(session);
		List<AnswerStat> stats = answers.stream().map(a -> {
			Map<String, Integer> fillers = countFillerWords(a.getFillerWords());
			int silenceCount = a.getSilencePeriods() == null ? 0 : a.getSilencePeriods().size();
			int totalMs = sumSilenceDuration(a.getSilencePeriods());
			return new AnswerStat(a, a.getQuestion().getSequenceOrder(), silenceCount, totalMs, fillers);
		}).toList();

		String answersText = buildCombinedAnswersText(stats);
		AiPromptRequest promptRequest = new AiPromptRequest(
			PromptTemplate.COMBINED_ANALYSIS_V1.getContent(),
			Map.of("answers", answersText),
			6000
		);

		return new AllAnalysisContext(session.getUuid(), report.getId(), promptRequest, stats);
	}

	/**
	 * 2단계: AI 응답 파싱 후 AnswerAnalysis 5개 + InterviewReport 저장.
	 */
	@Transactional
	public void saveAllAnalysesAndReport(Long sessionId, AllAnalysisContext ctx, AiResponse response) {
		Map<String, Object> parsed;
		try {
			parsed = LENIENT_MAPPER.readValue(response.content(), new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			throw new IllegalStateException("AI 응답 파싱 실패: " + e.getMessage(), e);
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> analysesJson = (List<Map<String, Object>>)parsed.get("a");
		String summary = (String)parsed.get("sum");

		Map<Integer, AnswerStat> statBySeq = ctx.answerStats().stream()
			.collect(Collectors.toMap(AnswerStat::sequenceOrder, s -> s));

		int totalSilence = 0, totalFiller = 0, totalLogic = 0, totalTotal = 0, n = 0;

		if (analysesJson != null) {
			for (Map<String, Object> item : analysesJson) {
				int seq = ((Number)item.get("i")).intValue();
				String review = (String)item.get("r");
				int logicScore = item.get("s") != null ? ((Number)item.get("s")).intValue() : 0;
				AnswerStat stat = statBySeq.get(seq);
				if (stat == null)
					continue;

				int silenceScore = calcSilenceScore(stat.totalSilenceDurationMs(), stat.silenceCount());
				int fillerScore = calcFillerScore(stat.fillerWordCounts());
				int totalScore = (int)Math.round((silenceScore + fillerScore + logicScore) / 3.0);

				analysisRepository.save(new AnswerAnalysis(
					stat.answer(), stat.totalSilenceDurationMs(), stat.silenceCount(),
					stat.fillerWordCounts(), review, logicScore, silenceScore, fillerScore, totalScore
				));

				totalSilence += silenceScore;
				totalFiller += fillerScore;
				totalLogic += logicScore;
				totalTotal += totalScore;
				n++;
			}
		}

		InterviewReport report = reportRepository.findById(ctx.reportId())
			.orElseThrow(() -> new IllegalStateException("리포트를 찾을 수 없습니다. sessionId=" + sessionId));

		int cnt = n > 0 ? n : 1;
		report.complete(
			summary != null ? summary.trim() : "",
			totalSilence / cnt, totalFiller / cnt, totalLogic / cnt, totalTotal / cnt
		);
	}

	// ── 리포트 실패 처리 ───────────────────────────────────────────────────────

	@Transactional
	public void markReportFailed(Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
		if (session == null)
			return;
		reportRepository.findBySession(session).ifPresent(InterviewReport::fail);
	}

	// ── 조회 API ───────────────────────────────────────────────────────────────

	public AnswerAnalysisResponse getByUuid(UUID analysisUuid, String userUuidStr) {
		AnswerAnalysis analysis = analysisRepository.findByUuid(analysisUuid)
			.orElseThrow(() -> new AnswerAnalysisNotFoundException("답변 분석을 찾을 수 없습니다."));

		InterviewSession session = analysis.getAnswer().getQuestion().getSession();
		if (!session.getUser().getUuid().equals(UUID.fromString(userUuidStr))) {
			throw new AnswerAnalysisNotFoundException("답변 분석을 찾을 수 없습니다.");
		}

		return AnswerAnalysisResponse.from(analysis);
	}

	public List<AnswerAnalysisResponse> getBySessionUuid(UUID sessionUuid, String userUuidStr) {
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

	public record AnswerStat(
		InterviewAnswer answer,
		int sequenceOrder,
		int silenceCount,
		int totalSilenceDurationMs,
		Map<String, Integer> fillerWordCounts
	) {
	}

	public record AllAnalysisContext(
		UUID sessionUuid,
		Long reportId,
		AiPromptRequest promptRequest,
		List<AnswerStat> answerStats
	) {
	}

	// ── 유틸 ───────────────────────────────────────────────────────────────────

	private String buildCombinedAnswersText(List<AnswerStat> stats) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stats.size(); i++) {
			AnswerStat stat = stats.get(i);
			String raw = stat.answer().getTranscript();
			int originalLen = raw != null ? raw.length() : 0;
			String transcript = raw != null ? raw : "";

			String lenLabel;
			if (originalLen > TRANSCRIPT_MAX_LEN) {
				transcript = transcript.substring(0, TRANSCRIPT_MAX_LEN);
				lenLabel = originalLen + "자→" + TRANSCRIPT_MAX_LEN + "자";
			} else {
				lenLabel = originalLen + "자";
			}

			Map<String, Integer> f = stat.fillerWordCounts();
			String silenceInfo = (stat.totalSilenceDurationMs() / 1000) + "s/" + stat.silenceCount() + "회";
			String fillerInfo = "음" + f.getOrDefault("음", 0) + " 어" + f.getOrDefault("어", 0) + " 그" + f.getOrDefault("그", 0);

			sb.append("[").append(stat.sequenceOrder()).append("] Q: ")
				.append(stat.answer().getQuestion().getContent()).append("\n");
			sb.append("A(").append(lenLabel).append("): ").append(transcript).append("\n");
			sb.append("침묵: ").append(silenceInfo).append(" | 습관어: ").append(fillerInfo);

			if (i < stats.size() - 1) {
				sb.append("\n\n");
			}
		}
		return sb.toString();
	}

	private Map<String, Integer> countFillerWords(List<String> fillerWords) {
		List<String> targets = List.of("음", "어", "그");
		if (fillerWords == null || fillerWords.isEmpty()) {
			return targets.stream().collect(Collectors.toMap(k -> k, k -> 0));
		}
		return targets.stream().collect(
			Collectors.toMap(k -> k, k -> (int)fillerWords.stream().filter(k::equals).count())
		);
	}

	private int sumSilenceDuration(List<TimePeriod> periods) {
		if (periods == null)
			return 0;
		return periods.stream().mapToInt(p -> p.endMs() - p.startMs()).sum();
	}

	private int calcSilenceScore(Integer totalSilenceDurationMs, Integer silenceCount) {
		int durationSec = totalSilenceDurationMs / 1000;
		int deduction = Math.max(0, durationSec - 5 * silenceCount);
		return Math.max(0, 100 - deduction);
	}

	private int calcFillerScore(Map<String, Integer> fillerWordCounts) {
		int totalFillers = fillerWordCounts.values().stream().mapToInt(Integer::intValue).sum();
		return Math.max(0, 100 - totalFillers);
	}

}
