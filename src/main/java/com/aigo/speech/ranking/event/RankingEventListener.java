package com.aigo.speech.ranking.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.aigo.speech.ranking.service.RankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventListener {
	private final RankingService rankingService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleInterviewCompleted (InterviewCompletedEvent event) {
		log.info(
			"[RankingEvent] 수신. userUuid={}, score={}",
			event.userUuid(), event.totalScore()
		);
		try {
			rankingService.updateRanking(event.userUuid(), event.totalScore(), event.jobTitle());
		} catch (Exception e) {
			/* 랭킹 실패가 면접 결과에 영향 XX */
			log.error(
				"[RankingEvent] 랭킹 업데이트 실패. userUuid={}, error={}",
				event.userUuid(), e.getMessage(), e
			);
		}
	}
}
