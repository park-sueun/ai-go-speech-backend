package com.aigo.speech.answer.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.aigo.speech.answer.dto.AllAnswersSubmittedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnswerAnalysisProcessor {

	private final AnswerAnalysisService answerAnalysisService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("answerExecutor")
	public void handle(AllAnswersSubmittedEvent event) {
		answerAnalysisService.generateAllAnalysesAndReport(event.sessionId());
	}
}
