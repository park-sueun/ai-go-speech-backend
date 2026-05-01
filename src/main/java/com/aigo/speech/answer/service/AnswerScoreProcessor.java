package com.aigo.speech.answer.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.aigo.speech.answer.dto.AnswerSubmittedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnswerScoreProcessor {

	private final AnswerScoreService answerScoreService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("answerExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle (AnswerSubmittedEvent event) {
		answerScoreService.saveScore(
			event.answerId(),
			event.silenceCount(),
			event.totalSilenceDuration(),
			event.answerDuration()
		);
	}
}
