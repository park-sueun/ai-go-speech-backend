package com.aigo.speech.ranking.repository;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RankingRepository {
	private final RedisTemplate<String, String> redisTemplate;
	private static final String RANKING_KEY = "ranking:weekly";

	public void save (UUID userUuid, int score) {
		String key = userUuid.toString();
		Double existing = redisTemplate.opsForZSet().score(RANKING_KEY, key);

		if (existing == null || existing < score) {
			redisTemplate.opsForZSet().add(RANKING_KEY, key, score);
			log.debug("랭킹 갱신[Redis]: uuid = {}, score = {}", userUuid, score);
		}
	}

	public void delete (UUID userUuid) {
		redisTemplate.opsForZSet().remove(RANKING_KEY, userUuid.toString());
	}

	/* 전체 초기화 - 주간 리셋 */
	public void clear () {
		redisTemplate.delete(RANKING_KEY);
	}

	/* 상위 N명 */
	public Set<ZSetOperations.TypedTuple<String>> getTopN (long n) {
		return redisTemplate.opsForZSet().rangeWithScores(RANKING_KEY, 0, n - 1);
	}

	public Long getMyRank (UUID userUuid) {
		Double myScore = getMyScore(userUuid);
		if (myScore == null) return null;
		// 내 점수보다 높은 사람 수 + 1 = 동점 처리된 내 순위
		Long above = redisTemplate.opsForZSet().count(RANKING_KEY, myScore + 1, Double.MAX_VALUE);
		return above != null ? above + 1 : 1L;
	}

	public Double getMyScore (UUID userUuid) {
		return redisTemplate.opsForZSet().score(RANKING_KEY, userUuid.toString());
	}

	/* 전체 사용자 수 */
	public long getTotalCount () {
		Long count = redisTemplate.opsForZSet().zCard(RANKING_KEY);
		return count != null ? count : 0L;
	}

	/* 특정 순위 범위 조회 */
	public Set<ZSetOperations.TypedTuple<String>> getRangeWithScore (long start, long end) {
		return redisTemplate.opsForZSet().rangeWithScores(RANKING_KEY, start, end);
	}
}
