package com.aigo.speech.ranking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aigo.speech.ranking.dto.RankingListResponse;
import com.aigo.speech.ranking.entity.RankingBackup;
import com.aigo.speech.ranking.repository.RankingBackupRepository;
import com.aigo.speech.ranking.repository.RankingRepository;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

	@Mock RankingRepository rankingRepository;
	@Mock RankingBackupRepository rankingBackupRepository;
	@Mock UserRepository userRepository;

	@InjectMocks
	RankingService rankingService;

	private UUID userUuid;
	private RankingBackup backup;

	@BeforeEach
	void setUp () {
		userUuid = UUID.randomUUID();
		backup = mockBackup(userUuid, 80, "서비스 기획자");
	}

	@Test
	@DisplayName("DB에서 랭킹 목록을 정상 반환한다")
	void getRankings_success () {
		given(rankingBackupRepository.findAllOrderByBestScoreDesc()).willReturn(List.of(backup));
		given(rankingBackupRepository.countHigherThan(userUuid)).willReturn(0L);

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.rankingItemResponseList()).hasSize(1);
		assertThat(response.rankingItemResponseList().get(0).rank()).isEqualTo(1);
		assertThat(response.rankingItemResponseList().get(0).bestScore()).isEqualTo(80);
		assertThat(response.myRankingResponse()).isNotNull();
		assertThat(response.myRankingResponse().rank()).isEqualTo(1L);
		assertThat(response.totalCount()).isEqualTo(1L);
	}

	@Test
	@DisplayName("면접 기록이 없는 유저는 myRanking이 null이다")
	void getRankings_noHistory_myRankingNull () {
		given(rankingBackupRepository.findAllOrderByBestScoreDesc()).willReturn(List.of());

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.myRankingResponse()).isNull();
		assertThat(response.rankingItemResponseList()).isEmpty();
		assertThat(response.totalCount()).isZero();
	}

	@Test
	@DisplayName("동점자는 같은 등수이고, 다음 등수는 건너뛴다 (85-77-77-75 → 1,2,2,4)")
	void getRankings_tiedScores_sameRank () {
		UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
		UUID u3 = UUID.randomUUID(), u4 = UUID.randomUUID();

		List<RankingBackup> all = List.of(
			mockBackup(u1, 85, null),
			mockBackup(u2, 77, null),
			mockBackup(u3, 77, null),
			mockBackup(u4, 75, null)
		);
		given(rankingBackupRepository.findAllOrderByBestScoreDesc()).willReturn(all);

		RankingListResponse response = rankingService.getRankings(userUuid);

		List<RankingListResponse.RankingItemResponse> list = response.rankingItemResponseList();
		assertThat(list).hasSize(4);
		assertThat(list.get(0).rank()).isEqualTo(1);
		assertThat(list.get(1).rank()).isEqualTo(2);
		assertThat(list.get(2).rank()).isEqualTo(2);
		assertThat(list.get(3).rank()).isEqualTo(4);
	}

	@Test
	@DisplayName("내 점수보다 높은 사람 수 기반으로 내 순위를 계산한다")
	void getRankings_myRankBasedOnCountAbove () {
		given(rankingBackupRepository.findAllOrderByBestScoreDesc()).willReturn(List.of(backup));
		given(rankingBackupRepository.countHigherThan(userUuid)).willReturn(2L); // 2명이 더 높음 → 3위

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.myRankingResponse().rank()).isEqualTo(3L);
	}

	@Test
	@DisplayName("처음 면접 완료 시 RankingBackup을 새로 생성한다")
	void updateRanking_firstTime_createNew () {
		User user = mock(User.class);
		given(userRepository.findByUuid(userUuid)).willReturn(Optional.of(user));
		given(rankingBackupRepository.findByUserUuid(userUuid)).willReturn(Optional.empty());

		RankingBackup newBackup = mock(RankingBackup.class);
		given(rankingBackupRepository.save(any(RankingBackup.class))).willReturn(newBackup);

		rankingService.updateRanking(userUuid, 64, "서비스 기획자");

		verify(rankingRepository).save(userUuid, 64);
		verify(rankingBackupRepository).save(any(RankingBackup.class));
		verify(newBackup).update(64);
	}

	@Test
	@DisplayName("기존 RankingBackup이 있으면 update를 호출한다")
	void updateRanking_existing_callsUpdate () {
		User user = mock(User.class);
		given(userRepository.findByUuid(userUuid)).willReturn(Optional.of(user));
		given(rankingBackupRepository.findByUserUuid(userUuid)).willReturn(Optional.of(backup));

		rankingService.updateRanking(userUuid, 90, "백엔드 개발자");

		verify(rankingRepository).save(userUuid, 90);
		verify(backup).update(90);
	}

	@Test
	@DisplayName("존재하지 않는 유저 UUID로 업데이트 시 예외가 발생한다")
	void updateRanking_userNotFound_throwsException () {
		given(userRepository.findByUuid(userUuid)).willReturn(Optional.empty());

		assertThatThrownBy(() -> rankingService.updateRanking(userUuid, 64, "기획자"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("유저 탈퇴 시 Redis와 DB에서 모두 삭제한다")
	void removeRanking_deletesFromBoth () {
		given(rankingBackupRepository.findByUserUuid(userUuid)).willReturn(Optional.of(backup));

		rankingService.deleteRanking(userUuid);

		verify(rankingRepository).delete(userUuid);
		verify(rankingBackupRepository).delete(backup);
	}

	private RankingBackup mockBackup (UUID uuid, int score, String jobTitle) {
		Profile profile = mock(Profile.class);
		lenient().when(profile.getNickname()).thenReturn("유저");
		lenient().when(profile.getProfileImageUrl()).thenReturn(null);

		User user = mock(User.class);
		lenient().when(user.getUuid()).thenReturn(uuid);
		lenient().when(user.getProfile()).thenReturn(profile);

		RankingBackup b = mock(RankingBackup.class);
		lenient().when(b.getUserUuid()).thenReturn(uuid);
		lenient().when(b.getBestScore()).thenReturn(score);
		lenient().when(b.getTotalSessionCount()).thenReturn(3);
		lenient().when(b.getJobTitle()).thenReturn(jobTitle);
		lenient().when(b.getUser()).thenReturn(user);
		return b;
	}
}
