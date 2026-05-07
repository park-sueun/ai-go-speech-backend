package com.aigo.speech.ranking.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.ranking.dto.RankingListResponse;
import com.aigo.speech.ranking.dto.RankingListResponse.RankingItemResponse;
import com.aigo.speech.ranking.entity.RankingBackup;
import com.aigo.speech.ranking.repository.RankingBackupRepository;
import com.aigo.speech.ranking.repository.RankingRepository;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {
	private final RankingRepository rankingRepository;
	private final RankingBackupRepository rankingBackupRepository;
	private final UserRepository userRepository;

	private static final int TOP_N = 100;

	public RankingListResponse getRankings (UUID UserUuid) {
		if (rankingRepository.getTotalCount() == 0) {
			log.warn("[Ranking] Redis 데이터없음 >> DB warm-up 시작");
			warmUpFromDb();
		}
		long totalCount = rankingRepository.getTotalCount();
		Set<ZSetOperations.TypedTuple<String>> topSet = rankingRepository.getTopN(TOP_N);
		List<UUID> uuids = topSet.stream().map(t -> UUID.fromString(Objects.requireNonNull(t.getValue()))).toList();

		Map<UUID, User> userMap = fetchUserMap(uuids);
		Map<UUID, RankingBackup> backupMap = fetchBackupMap(uuids);

		List<RankingItemResponse> ranking = new ArrayList<>();
		long rank = 1;
		for (ZSetOperations.TypedTuple<String> tuple : topSet) {
			UUID uuid = UUID.fromString(Objects.requireNonNull(tuple.getValue()));
			double score = Objects.requireNonNull(tuple.getScore());

			User user = userMap.get(uuid);
			if (user == null) {
				rank++;
				continue;
			}

			Profile profile = user.getProfile();
			int totalSessions = backupMap.containsKey(uuid) ? backupMap.get(uuid).getTotalSessionCount() : 0;
			String jobTitle = backupMap.containsKey(uuid)
				? backupMap.get(uuid).getJobTitle() : null;
			ranking.add(new RankingItemResponse(
				rank++,
				profile.getNickname(),
				jobTitle,
				(int)score,
				totalSessions
			));
		}

		RankingListResponse.MyRankingResponse myRanking = buildMyRanking(UserUuid, userMap, backupMap);
		log.info("[Ranking] 조회 완료. myUuid={}, totalCount={}", UserUuid, totalCount);
		return new RankingListResponse(myRanking, ranking, totalCount);
	}

	@Transactional
	public void updateRanking (UUID userUuid, int score, String jobTitle) {
		rankingRepository.save(userUuid, score);
		User user = userRepository.findByUuid(userUuid)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

		RankingBackup backup = rankingBackupRepository.findByUserUuid(userUuid)
			.orElseGet(() -> rankingBackupRepository.save(
				RankingBackup.builder()
					.user(user)
					.userUuid(userUuid)
					.jobTitle(jobTitle)
					.bestScore(0)
					.totalSessionCount(0)
					.build()
			));
		backup.update(score);
		backup.updateJobTitle(jobTitle);
		log.info(
			"[Ranking] 업데이트 완료. uuid={}, score={}, bestScore={}",
			userUuid, score, backup.getBestScore()
		);
	}

	@Transactional
	public void deleteRanking (UUID userUuid) {
		rankingRepository.delete(userUuid);
		rankingBackupRepository.findByUserUuid(userUuid)
			.ifPresent(rankingBackupRepository::delete);
		log.info("[Ranking] 유저 랭킹 삭제. uuid={}", userUuid);
	}

	private void warmUpFromDb () {
		List<RankingBackup> all = rankingBackupRepository.findAllOrderByBestScoreDesc();
		for (RankingBackup backup : all) {
			rankingRepository.save(backup.getUserUuid(), backup.getBestScore());
		}
		log.info("[Ranking] warm-up 완료. count={}", all.size());
	}

	/* UUID 목록으로 User 일괄 조회 >> Map 반환 */
	private Map<UUID, User> fetchUserMap (List<UUID> uuids) {
		List<User> users = userRepository.findAllByUuidIn(uuids);
		Map<UUID, User> map = new HashMap<>();
		for (User u : users) {
			map.put(u.getUuid(), u);
		}
		return map;
	}

	/* UUID 목록으로 RankingBackup 조회 >> Map 반환 */
	private Map<UUID, RankingBackup> fetchBackupMap (List<UUID> uuids) {
		List<RankingBackup> backups = rankingBackupRepository.findAllByUserUuidIn(uuids);
		Map<UUID, RankingBackup> map = new HashMap<>();
		for (RankingBackup b : backups) {
			map.put(b.getUserUuid(), b);
		}
		return map;
	}

	/* 내 순위 정보 빌드 */
	private RankingListResponse.MyRankingResponse buildMyRanking (
		UUID myUuid,
		Map<UUID, User> userMap,
		Map<UUID, RankingBackup> backupMap
	) {
		Long myRank = rankingRepository.getMyRank(myUuid);
		Double myScore = rankingRepository.getMyScore(myUuid);

		if (myRank == null || myScore == null) {
			return null;
		}

		User me = userMap.computeIfAbsent(
			myUuid, uuid ->
				userRepository.findByUuid(uuid).orElse(null)
		);

		if (me == null)
			return null;

		Profile profile = me.getProfile();
		int totalSessions = backupMap.containsKey(myUuid)
			? backupMap.get(myUuid).getTotalSessionCount() : 0;
		String jobTitle = backupMap.containsKey(myUuid)
			? backupMap.get(myUuid).getJobTitle() : null;

		return new RankingListResponse.MyRankingResponse(
			myRank,
			profile.getNickname(),
			jobTitle,
			myScore.intValue(),
			totalSessions
		);
	}
}
