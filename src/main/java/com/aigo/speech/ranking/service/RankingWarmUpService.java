package com.aigo.speech.ranking.service;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.aigo.speech.ranking.entity.RankingBackup;
import com.aigo.speech.ranking.repository.RankingBackupRepository;
import com.aigo.speech.ranking.repository.RankingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingWarmUpService {
	private final RankingRepository rankingRepository;
	private final RankingBackupRepository backupRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void warmUp () {
		if (rankingRepository.getTotalCount() > 0) {
			log.info(
				"[Ranking] warm-up skip — Redis에 데이터 존재. count={}",
				rankingRepository.getTotalCount()
			);
			return;
		}
		List<RankingBackup> all = backupRepository.findAllOrderByBestScoreDesc();
		if (all.isEmpty()) {
			log.info("[Ranking] warm-up skip — DB에 랭킹 데이터 없음");
			return;
		}
		for (RankingBackup backup : all) {
			rankingRepository.save(backup.getUserUuid(), backup.getBestScore());
		}
		
		log.info("[Ranking] warm-up 완료. {}명 Redis 저장", all.size());
	}
}
