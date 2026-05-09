package com.aigo.speech.ranking.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RankingRepositoryTest {

	@Mock
	RedisTemplate<String, String> redisTemplate;

	@Mock
	ZSetOperations<String, String> zSetOperations;

	@InjectMocks
	RankingRepository rankingRepository;

	private static final String RANKING_KEY = "ranking:weekly";
	private UUID userUuid;

	@BeforeEach
	void setUp () {
		userUuid = UUID.randomUUID();
		lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
	}

	@Test
	@DisplayName("기존 점수가 없을 때 새 점수를 저장한다")
	void save_newEntry () {
		given(zSetOperations.score(eq(RANKING_KEY), eq(userUuid.toString())))
			.willReturn(null);

		rankingRepository.save(userUuid, 80);

		verify(zSetOperations).add(RANKING_KEY, userUuid.toString(), 80.0);
	}

	@Test
	@DisplayName("새 점수가 기존보다 높으면 갱신한다")
	void save_higherScore_updates () {
		given(zSetOperations.score(eq(RANKING_KEY), eq(userUuid.toString())))
			.willReturn(60.0);

		rankingRepository.save(userUuid, 80);

		verify(zSetOperations).add(RANKING_KEY, userUuid.toString(), 80.0);
	}

	@Test
	@DisplayName("새 점수가 기존보다 낮으면 갱신하지 않는다")
	void save_lowerScore_noUpdate () {
		given(zSetOperations.score(eq(RANKING_KEY), eq(userUuid.toString())))
			.willReturn(90.0);

		rankingRepository.save(userUuid, 70);

		verify(zSetOperations, org.mockito.Mockito.never())
			.add(anyString(), anyString(), anyDouble());
	}

	@Test
	@DisplayName("내 점수보다 높은 사람이 2명이면 3위를 반환한다")
	void getMyRank_returnsCountBasedRank () {
		given(zSetOperations.score(eq(RANKING_KEY), eq(userUuid.toString())))
			.willReturn(80.0);
		given(zSetOperations.count(eq(RANKING_KEY), eq(81.0), eq(Double.MAX_VALUE)))
			.willReturn(2L); // 81점 이상 2명 → 3위

		Long rank = rankingRepository.getMyRank(userUuid);

		assertThat(rank).isEqualTo(3L);
	}

	@Test
	@DisplayName("내 점수가 최고점이면 1위를 반환한다")
	void getMyRank_topScore_returnsFirst () {
		given(zSetOperations.score(eq(RANKING_KEY), eq(userUuid.toString())))
			.willReturn(100.0);
		given(zSetOperations.count(eq(RANKING_KEY), eq(101.0), eq(Double.MAX_VALUE)))
			.willReturn(0L);

		Long rank = rankingRepository.getMyRank(userUuid);

		assertThat(rank).isEqualTo(1L);
	}

	@Test
	@DisplayName("랭킹에 없으면 null을 반환한다")
	void getMyRank_notFound_returnsNull () {
		given(zSetOperations.score(eq(RANKING_KEY), eq(userUuid.toString())))
			.willReturn(null);

		Long rank = rankingRepository.getMyRank(userUuid);

		assertThat(rank).isNull();
	}

	@Test
	@DisplayName("유저 UUID로 랭킹에서 삭제한다")
	void remove_callsRedisRemove () {
		rankingRepository.delete(userUuid);

		verify(zSetOperations).remove(RANKING_KEY, userUuid.toString());
	}

	@Test
	@DisplayName("전체 인원 수를 반환한다")
	void getTotalCount_returnsCount () {
		given(zSetOperations.zCard(RANKING_KEY)).willReturn(5L);

		long count = rankingRepository.getTotalCount();

		assertThat(count).isEqualTo(5L);
	}

	@Test
	@DisplayName("Redis가 null 반환하면 0을 반환한다")
	void getTotalCount_null_returnsZero () {
		given(zSetOperations.zCard(RANKING_KEY)).willReturn(null);

		long count = rankingRepository.getTotalCount();

		assertThat(count).isZero();
	}
}