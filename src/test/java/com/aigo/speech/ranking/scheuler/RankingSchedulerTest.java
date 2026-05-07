package com.aigo.speech.ranking.scheuler;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aigo.speech.ranking.repository.RankingRepository;

@ExtendWith(MockitoExtension.class)
class RankingSchedulerTest {

	@Mock
	RankingRepository rankingRepository;

	@InjectMocks
	RankingScheduler rankingScheduler;

	@Test
	@DisplayName("주간 리셋 시 Redis 랭킹을 초기화한다")
	void resetWeeklyRanking_callsClear () {
		given(rankingRepository.getTotalCount()).willReturn(50L);

		rankingScheduler.resetWeeklyRanking();

		verify(rankingRepository).clear();
	}
}