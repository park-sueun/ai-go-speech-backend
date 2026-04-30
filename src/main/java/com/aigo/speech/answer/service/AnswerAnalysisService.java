package com.aigo.speech.answer.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.repository.InterviewSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalysisService {

	private final InterviewSessionRepository sessionRepository;
	private final SseEmitterService sseEmitterService;

	@Async("interviewExecutor")
	@Transactional(readOnly = true)
	public void analyzeSessionAsync (Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new IllegalStateException("면접 세션을 찾을 수 없습니다: " + sessionId));

		UUID sessionUuid = session.getUuid();

		try {
			// TODO: 세션 내 모든 답변 종합 분석 로직 구현

			log.info("[SessionAnalysis] 종합 분석 완료. sessionId={}", sessionId);

			sseEmitterService.sendEvent(
				sessionUuid, "SESSION_ANALYSIS_DONE",
				Map.of("sessionUuid", sessionUuid.toString())
			);
			sseEmitterService.complete(sessionUuid);

		} catch (Exception e) {
			log.error("[SessionAnalysis] 종합 분석 실패. sessionId={}, error={}", sessionId, e.getMessage());
			sseEmitterService.sendEvent(sessionUuid, "ERROR", Map.of("message", "종합 분석에 실패했습니다."));
			sseEmitterService.complete(sessionUuid);
		}
	}
}
