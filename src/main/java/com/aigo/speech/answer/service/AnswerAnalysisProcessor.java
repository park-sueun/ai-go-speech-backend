package com.aigo.speech.answer.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.aigo.speech.answer.dto.AnswerSubmittedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnswerAnalysisProcessor {

	private final AnswerAnalysisService answerAnalysisService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("answerExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(AnswerSubmittedEvent event) {
		Long sessionId = answerAnalysisService.createAnalysis(
			event.answerId(),
			event.silenceCount(),
			event.totalSilenceDuration()
		);

		if (sessionId != null) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					answerAnalysisService.generateReportAsync(sessionId);
				}
			});
		}
	}
}
