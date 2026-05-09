package com.aigo.speech.ranking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

	public RankingListResponse getRankings (UUID userUuid) {
		List<RankingBackup> all = rankingBackupRepository.findAllOrderByBestScoreDesc();
		long totalCount = all.size();

		List<RankingItemResponse> ranking = buildRankingList(all);
		RankingListResponse.MyRankingResponse myRanking = buildMyRanking(userUuid, all);

		log.info("[Ranking] 조회 완료. myUuid={}, totalCount={}", userUuid, totalCount);
		return new RankingListResponse(myRanking, ranking, totalCount);
	}

	private List<RankingItemResponse> buildRankingList (List<RankingBackup> all) {
		List<RankingItemResponse> ranking = new ArrayList<>();
		long position = 0;
		long currentRank = 0;
		int prevScore = Integer.MIN_VALUE;
		int limit = Math.min(all.size(), TOP_N);

		for (int i = 0; i < limit; i++) {
			RankingBackup backup = all.get(i);
			position++;

			if (backup.getBestScore() != prevScore) {
				currentRank = position;
				prevScore = backup.getBestScore();
			}

			Profile profile = backup.getUser().getProfile();
			ranking.add(new RankingItemResponse(
				currentRank,
				profile.getNickname(),
				profile.getProfileImageUrl(),
				backup.getJobTitle(),
				backup.getBestScore(),
				backup.getTotalSessionCount()
			));
		}
		return ranking;
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

	/* 내 순위 정보 빌드 */
	private RankingListResponse.MyRankingResponse buildMyRanking (UUID myUuid, List<RankingBackup> all) {
		RankingBackup myBackup = all.stream()
			.filter(b -> b.getUserUuid().equals(myUuid))
			.findFirst()
			.orElse(null);

		if (myBackup == null) return null;

		long above = rankingBackupRepository.countHigherThan(myUuid);
		Profile profile = myBackup.getUser().getProfile();

		return new RankingListResponse.MyRankingResponse(
			above + 1,
			profile.getNickname(),
			profile.getProfileImageUrl(),
			myBackup.getJobTitle(),
			myBackup.getBestScore(),
			myBackup.getTotalSessionCount()
		);
	}
}
