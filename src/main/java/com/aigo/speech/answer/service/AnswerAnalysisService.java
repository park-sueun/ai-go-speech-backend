package com.aigo.speech.answer.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.prompt.PromptTemplate;
import com.aigo.speech.ai.service.AiService;
import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.answer.repository.AnswerAnalysisRepository;
import com.aigo.speech.answer.repository.InterviewAnswerRepository;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.repository.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerAnalysisService {

	private final InterviewAnswerRepository answerRepository;
	private final AnswerAnalysisRepository analysisRepository;
	private final InterviewSessionRepository sessionRepository;
	private final InterviewReportRepository reportRepository;
	private final InterviewQuestionRepository questionRepository;
	private final AiService aiService;
	private final ObjectMapper objectMapper;
	private final SseEmitterService sseEmitterService;

	/**
	 * 답변 분석 생성. 해당 세션의 마지막 분석이면 sessionId 반환, 아니면 null 반환.
	 */
	@Transactional
	public Long createAnalysis (Long answerId, Integer silenceCount, Integer totalSilenceDuration) {
		InterviewAnswer answer = answerRepository.findById(answerId)
			.orElseThrow(() -> new IllegalStateException("답변을 찾을 수 없습니다: " + answerId));

		InterviewQuestion question = answer.getQuestion();
		InterviewSession session = question.getSession();

		Map<String, Integer> fillerWordCounts = countFillerWords(answer.getFillerWords());

		String aiReview = null;
		Integer logicScore = 0;
		try {
			AiPromptRequest request = AiPromptRequest.of(
				PromptTemplate.ANSWER_ANALYSIS_V1.getContent(),
				Map.of(
					"question", orEmpty(question.getContent()),
					"transcript", orEmpty(answer.getTranscript())
				)
			);
			AiResponse response = aiService.complete(request);
			Map<String, Object> parsed = objectMapper.readValue(
				response.content(), new TypeReference<Map<String, Object>>() {
				}
			);
			aiReview = (String)parsed.get("aiReview");
			logicScore = ((Number)parsed.get("logicScore")).intValue();
		} catch (Exception e) {
			log.error("[AnswerAnalysis] AI 분석 실패. answerId={}, error={}", answerId, e.getMessage());
		}

		int silenceScore = calcSilenceScore(totalSilenceDuration, silenceCount);
		int fillerScore = calcFillerScore(fillerWordCounts);
		int totalScore = (int)Math.round((silenceScore + fillerScore + logicScore) / 3.0);

		analysisRepository.save(
			new AnswerAnalysis(
				answer, totalSilenceDuration, silenceCount, fillerWordCounts, aiReview,
				logicScore, silenceScore, fillerScore, totalScore
			)
		);

		log.info("[AnswerAnalysis] 분석 저장 완료. answerId={}", answerId);

		long analysisCount = analysisRepository.countByAnswerQuestionSession(session);
		long totalQuestions = questionRepository.countBySession(session);

		return analysisCount >= totalQuestions ? session.getId() : null;
	}

	@Async("answerExecutor")
	@Transactional
	public void generateReportAsync (Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new IllegalStateException("면접 세션을 찾을 수 없습니다: " + sessionId));

		UUID sessionUuid = session.getUuid();

		try {
			if (reportRepository.existsBySession(session)) {
				log.info("[SessionReport] 이미 리포트가 존재합니다. sessionId={}", sessionId);
				return;
			}

			List<AnswerAnalysis> analyses = analysisRepository.findBySessionWithDetails(session);

			String analysesText = buildAnalysesText(analyses);

			AiPromptRequest request = AiPromptRequest.of(
				PromptTemplate.SESSION_REPORT_V1.getContent(),
				Map.of("analyses", analysesText)
			);
			AiResponse response = aiService.complete(request);
			String aiSummary = response.content().trim();

			int avgSilenceScore = avgScore(analyses, AnswerAnalysis::getSilenceScore);
			int avgFillerScore = avgScore(analyses, AnswerAnalysis::getFillerScore);
			int avgLogicScore = avgScore(analyses, AnswerAnalysis::getLogicScore);
			int avgTotalScore = avgScore(analyses, AnswerAnalysis::getTotalScore);

			reportRepository.save(new InterviewReport(
				session, aiSummary,
				avgSilenceScore, avgFillerScore, avgLogicScore, avgTotalScore
			));

			log.info("[SessionReport] 리포트 생성 완료. sessionId={}", sessionId);

			sseEmitterService.sendEvent(
				sessionUuid, "SESSION_ANALYSIS_DONE",
				Map.of("sessionUuid", sessionUuid.toString())
			);
			sseEmitterService.complete(sessionUuid);

		} catch (Exception e) {
			log.error("[SessionReport] 리포트 생성 실패. sessionId={}, error={}", sessionId, e.getMessage());
			sseEmitterService.sendEvent(sessionUuid, "ERROR", Map.of("message", "리포트 생성에 실패했습니다."));
			sseEmitterService.complete(sessionUuid);
		}
	}

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
			sb.append("질문 ").append(q.getSequenceOrder()).append(": ").append(q.getContent()).append("\n");
			sb.append("답변: ").append(orEmpty(aa.getAnswer().getTranscript())).append("\n");
			sb.append("AI 분석: ").append(orEmpty(aa.getAiReview())).append("\n");
			sb.append("논리 점수: ").append(aa.getLogicScore()).append("점\n\n");
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
