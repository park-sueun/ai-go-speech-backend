package com.aigo.speech.answer.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.aigo.speech.answer.dto.AnswerSubmittedEvent;
import com.aigo.speech.answer.service.AnswerAnalysisService.AiAnalysisResult;
import com.aigo.speech.answer.service.AnswerAnalysisService.AnswerData;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnswerAnalysisProcessor {

	private final AnswerAnalysisService answerAnalysisService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("answerExecutor")
	public void handle(AnswerSubmittedEvent event) {
		// 1단계: DB 읽기 (짧은 readOnly 트랜잭션 — 즉시 커넥션 반납)
		AnswerData data = answerAnalysisService.loadAnswerData(
			event.answerId(), event.silenceCount(), event.totalSilenceDuration()
		);

		// 2단계: AI 호출 (트랜잭션 없음 — DB 커넥션 미점유)
		AiAnalysisResult aiResult = answerAnalysisService.performAiAnalysis(data);

		// 3단계: DB 저장 (짧은 쓰기 트랜잭션)
		Long sessionId = answerAnalysisService.saveAnalysisResult(data, aiResult);

		if (sessionId != null) {
			answerAnalysisService.generateReportAsync(sessionId);
		}
	}
}
