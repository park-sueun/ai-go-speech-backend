package com.aigo.speech.ranking.service;

import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aigo.speech.ranking.entity.RankingBackup;
import com.aigo.speech.ranking.repository.RankingBackupRepository;
import com.aigo.speech.ranking.repository.RankingRepository;

@ExtendWith(MockitoExtension.class)
class RankingWarmUpServiceTest {
	@Mock
	RankingRepository rankingRepository;
	@Mock
	RankingBackupRepository rankingBackupRepository;

	@InjectMocks
	RankingWarmUpService rankingWarmUpService;

	@Test
	@DisplayName("Redis가 비어있고 DB에 데이터가 있으면 warm-up을 수행한다")
	void warmUp_redisEmpty_loadsFromDb () {
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();

		RankingBackup b1 = mock(RankingBackup.class);
		given(b1.getUserUuid()).willReturn(uuid1);
		given(b1.getBestScore()).willReturn(80);

		RankingBackup b2 = mock(RankingBackup.class);
		given(b2.getUserUuid()).willReturn(uuid2);
		given(b2.getBestScore()).willReturn(60);

		given(rankingRepository.getTotalCount()).willReturn(0L);
		given(rankingBackupRepository.findAllOrderByBestScoreDesc())
			.willReturn(List.of(b1, b2));

		rankingWarmUpService.warmUp();

		verify(rankingRepository).save(uuid1, 80);
		verify(rankingRepository).save(uuid2, 60);
	}

	@Test
	@DisplayName("Redis에 이미 데이터가 있으면 warm-up을 건너뛴다")
	void warmUp_redisHasData_skips () {
		given(rankingRepository.getTotalCount()).willReturn(5L);

		rankingWarmUpService.warmUp();

		verify(rankingBackupRepository, never()).findAllOrderByBestScoreDesc();
		verify(rankingRepository, never()).save(any(), anyInt());
	}

	@Test
	@DisplayName("DB에도 데이터가 없으면 warm-up을 건너뛴다")
	void warmUp_dbEmpty_skips () {
		given(rankingRepository.getTotalCount()).willReturn(0L);
		given(rankingBackupRepository.findAllOrderByBestScoreDesc())
			.willReturn(List.of());

		rankingWarmUpService.warmUp();

		verify(rankingRepository, never()).save(any(), anyInt());
	}
}