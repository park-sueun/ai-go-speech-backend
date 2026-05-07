package com.aigo.speech.ranking.scheuler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aigo.speech.ranking.repository.RankingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {
	private final RankingRepository rankingRepository;

	@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
	public void resetWeeklyRanking () {
		long before = rankingRepository.getTotalCount();
		rankingRepository.clear();
		log.info("[RankingScheduler] 주간 랭킹 초기화 완료. 삭제 인원={}", before);
	}
}
