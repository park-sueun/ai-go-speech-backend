package com.aigo.speech.ranking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

	private static final LocalDate CURRENT_MONDAY = LocalDate.now().with(DayOfWeek.MONDAY);

	private UUID userUuid;
	private RankingBackup myBackup;

	@BeforeEach
	void setUp () {
		userUuid = UUID.randomUUID();
		myBackup = mockBackup(userUuid, 80, "서비스 기획자");
	}

	@Test
	@DisplayName("이번 주 데이터만 랭킹 목록으로 반환한다")
	void getRankings_returnsWeeklyData () {
		given(rankingBackupRepository.findWeeklyTopOrderByWeeklyScoreDesc(CURRENT_MONDAY))
			.willReturn(List.of(myBackup));
		given(rankingBackupRepository.countHigherWeeklyScore(CURRENT_MONDAY, 80)).willReturn(0L);

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.rankingItemResponseList()).hasSize(1);
		assertThat(response.rankingItemResponseList().get(0).rank()).isEqualTo(1);
		assertThat(response.rankingItemResponseList().get(0).bestScore()).isEqualTo(80);
		assertThat(response.myRankingResponse()).isNotNull();
		assertThat(response.myRankingResponse().rank()).isEqualTo(1L);
		assertThat(response.totalCount()).isEqualTo(1L);
	}

	@Test
	@DisplayName("이번 주 기록이 없으면 랭킹 목록이 비어있고 myRanking은 null이다")
	void getRankings_noWeeklyData_emptyResult () {
		given(rankingBackupRepository.findWeeklyTopOrderByWeeklyScoreDesc(CURRENT_MONDAY))
			.willReturn(List.of());

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.rankingItemResponseList()).isEmpty();
		assertThat(response.myRankingResponse()).isNull();
		assertThat(response.totalCount()).isZero();
	}

	@Test
	@DisplayName("동점자는 같은 등수이고, 다음 등수는 건너뛴다 (85-77-77-75 → 1,2,2,4)")
	void getRankings_tiedScores_sameRank () {
		UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
		UUID u3 = UUID.randomUUID(), u4 = UUID.randomUUID();

		List<RankingBackup> weekly = List.of(
			mockBackup(u1, 85, null),
			mockBackup(u2, 77, null),
			mockBackup(u3, 77, null),
			mockBackup(u4, 75, null)
		);
		given(rankingBackupRepository.findWeeklyTopOrderByWeeklyScoreDesc(CURRENT_MONDAY)).willReturn(weekly);

		RankingListResponse response = rankingService.getRankings(userUuid);

		List<RankingListResponse.RankingItemResponse> list = response.rankingItemResponseList();
		assertThat(list).hasSize(4);
		assertThat(list.get(0).rank()).isEqualTo(1);
		assertThat(list.get(1).rank()).isEqualTo(2);
		assertThat(list.get(2).rank()).isEqualTo(2);
		assertThat(list.get(3).rank()).isEqualTo(4);
	}

	@Test
	@DisplayName("내 순위는 이번 주 나보다 높은 점수 수 + 1이다")
	void getRankings_myRankBasedOnWeeklyCount () {
		given(rankingBackupRepository.findWeeklyTopOrderByWeeklyScoreDesc(CURRENT_MONDAY))
			.willReturn(List.of(myBackup));
		given(rankingBackupRepository.countHigherWeeklyScore(CURRENT_MONDAY, 80)).willReturn(2L);

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.myRankingResponse().rank()).isEqualTo(3L);
	}

	@Test
	@DisplayName("면접 완료 시 bestScore와 weeklyScore를 모두 갱신한다")
	void updateRanking_updatesWeeklyScore () {
		User user = mock(User.class);
		given(userRepository.findByUuid(userUuid)).willReturn(Optional.of(user));
		given(rankingBackupRepository.findByUserUuid(userUuid)).willReturn(Optional.of(myBackup));

		rankingService.updateRanking(userUuid, 90, "백엔드 개발자");

		verify(myBackup).update(90);
		verify(myBackup).updateWeeklyScore(90);
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
		verify(newBackup).updateWeeklyScore(64);
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
		given(rankingBackupRepository.findByUserUuid(userUuid)).willReturn(Optional.of(myBackup));

		rankingService.deleteRanking(userUuid);

		verify(rankingRepository).delete(userUuid);
		verify(rankingBackupRepository).delete(myBackup);
	}

	private RankingBackup mockBackup (UUID uuid, int weeklyScore, String jobTitle) {
		Profile profile = mock(Profile.class);
		lenient().when(profile.getNickname()).thenReturn("유저");
		lenient().when(profile.getProfileImageUrl()).thenReturn(null);

		User user = mock(User.class);
		lenient().when(user.getUuid()).thenReturn(uuid);
		lenient().when(user.getProfile()).thenReturn(profile);

		RankingBackup b = mock(RankingBackup.class);
		lenient().when(b.getUserUuid()).thenReturn(uuid);
		lenient().when(b.getWeeklyScore()).thenReturn(weeklyScore);
		lenient().when(b.getTotalSessionCount()).thenReturn(3);
		lenient().when(b.getJobTitle()).thenReturn(jobTitle);
		lenient().when(b.getUser()).thenReturn(user);
		return b;
	}
}
