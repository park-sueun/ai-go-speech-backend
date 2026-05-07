package com.aigo.speech.ranking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ZSetOperations;

import com.aigo.speech.ranking.dto.RankingListResponse;
import com.aigo.speech.ranking.entity.RankingBackup;
import com.aigo.speech.ranking.repository.RankingBackupRepository;
import com.aigo.speech.ranking.repository.RankingRepository;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

	@Mock
	RankingRepository rankingRepository;
	@Mock
	RankingBackupRepository rankingBackupRepository;
	@Mock
	UserRepository userRepository;

	@InjectMocks
	RankingService rankingService;

	private UUID userUuid;
	private User user;
	private Profile profile;
	private RankingBackup backup;

	@BeforeEach
	void setUp () {
		userUuid = UUID.randomUUID();

		profile = mock(Profile.class);
		lenient().when(profile.getNickname()).thenReturn("아무개");
		lenient().when(profile.getProfileImageUrl()).thenReturn(null);

		user = mock(User.class);
		lenient().when(user.getUuid()).thenReturn(userUuid);
		lenient().when(user.getProfile()).thenReturn(profile);
		lenient().when(user.getId()).thenReturn(1L);

		backup = mock(RankingBackup.class);
		lenient().when(backup.getUserUuid()).thenReturn(userUuid);
		lenient().when(backup.getTotalSessionCount()).thenReturn(3);
		lenient().when(backup.getJobTitle()).thenReturn("서비스 기획자");
		lenient().when(backup.getBestScore()).thenReturn(80);
	}

	@Test
	@DisplayName("Redis에 데이터가 있으면 정상적으로 랭킹 목록을 반환한다")
	void getRankings_success () {
		ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
		given(tuple.getValue()).willReturn(userUuid.toString());
		given(tuple.getScore()).willReturn(80.0);

		Set<ZSetOperations.TypedTuple<String>> topSet = new LinkedHashSet<>();
		topSet.add(tuple);

		given(rankingRepository.getTotalCount()).willReturn(1L);
		given(rankingRepository.getTopN(100)).willReturn(topSet);
		given(rankingRepository.getMyRank(userUuid)).willReturn(1L);
		given(rankingRepository.getMyScore(userUuid)).willReturn(80.0);

		given(userRepository.findAllByUuidIn(anyList())).willReturn(List.of(user));
		given(rankingBackupRepository.findAllByUserUuidIn(anyList())).willReturn(List.of(backup));

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.rankingItemResponseList()).hasSize(1);
		assertThat(response.rankingItemResponseList().get(0).rank()).isEqualTo(1);
		assertThat(response.rankingItemResponseList().get(0).nickname()).isEqualTo("아무개");
		assertThat(response.rankingItemResponseList().get(0).bestScore()).isEqualTo(80);
		assertThat(response.myRankingResponse()).isNotNull();
		assertThat(response.myRankingResponse().rank()).isEqualTo(1L);
		assertThat(response.totalCount()).isEqualTo(1L);
	}

	@Test
	@DisplayName("Redis가 비어있으면 DB warm-up 후 랭킹을 반환한다")
	void getRankings_redisEmpty_warmUp () {
		given(rankingRepository.getTotalCount())
			.willReturn(0L)
			.willReturn(1L);

		given(rankingBackupRepository.findAllOrderByBestScoreDesc())
			.willReturn(List.of(backup));

		ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
		given(tuple.getValue()).willReturn(userUuid.toString());
		given(tuple.getScore()).willReturn(80.0);
		Set<ZSetOperations.TypedTuple<String>> topSet = new LinkedHashSet<>(Set.of(tuple));

		given(rankingRepository.getTopN(100)).willReturn(topSet);
		given(rankingRepository.getMyRank(userUuid)).willReturn(1L);
		given(rankingRepository.getMyScore(userUuid)).willReturn(80.0);
		given(userRepository.findAllByUuidIn(anyList())).willReturn(List.of(user));
		given(rankingBackupRepository.findAllByUserUuidIn(anyList())).willReturn(List.of(backup));

		rankingService.getRankings(userUuid);

		verify(rankingRepository).save(backup.getUserUuid(), backup.getBestScore());
	}

	@Test
	@DisplayName("면접 기록이 없는 유저는 myRanking이 null이다")
	void getRankings_noHistory_myRankingNull () {
		given(rankingRepository.getTotalCount()).willReturn(0L);
		given(rankingBackupRepository.findAllOrderByBestScoreDesc()).willReturn(List.of());
		given(rankingRepository.getTopN(100)).willReturn(new LinkedHashSet<>());
		given(rankingRepository.getMyRank(userUuid)).willReturn(null);
		given(rankingRepository.getMyScore(userUuid)).willReturn(null);

		RankingListResponse response = rankingService.getRankings(userUuid);

		assertThat(response.myRankingResponse()).isNull();
		assertThat(response.rankingItemResponseList()).isEmpty();
	}

	@Test
	@DisplayName("처음 면접 완료 시 RankingBackup을 새로 생성한다")
	void updateRanking_firstTime_createNew () {
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

		org.junit.jupiter.api.Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> rankingService.updateRanking(userUuid, 64, "기획자")
		);
	}

	@Test
	@DisplayName("유저 탈퇴 시 Redis와 DB에서 모두 삭제한다")
	void removeRanking_deletesFromBoth () {
		given(rankingBackupRepository.findByUserUuid(userUuid))
			.willReturn(Optional.of(backup));

		rankingService.deleteRanking(userUuid);

		verify(rankingRepository).delete(userUuid);
		verify(rankingBackupRepository).delete(backup);
	}
}