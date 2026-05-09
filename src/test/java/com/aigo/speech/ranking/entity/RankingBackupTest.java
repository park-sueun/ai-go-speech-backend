package com.aigo.speech.ranking.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RankingBackupTest {
	@Test
	@DisplayName("새 점수가 기존보다 높으면 bestScore가 갱신된다")
	void update_higherScore_updatesBestScore () {
		RankingBackup backup = RankingBackup.builder()
			.user(mock(com.aigo.speech.user.entity.User.class))
			.userUuid(java.util.UUID.randomUUID())
			.bestScore(60)
			.totalSessionCount(1)
			.build();

		backup.update(80);

		assertThat(backup.getBestScore()).isEqualTo(80);
		assertThat(backup.getTotalSessionCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("새 점수가 기존보다 낮으면 bestScore가 유지된다")
	void update_lowerScore_keepsBestScore () {
		RankingBackup backup = RankingBackup.builder()
			.user(mock(com.aigo.speech.user.entity.User.class))
			.userUuid(java.util.UUID.randomUUID())
			.bestScore(90)
			.totalSessionCount(2)
			.build();

		backup.update(60);

		assertThat(backup.getBestScore()).isEqualTo(90); // 유지
		assertThat(backup.getTotalSessionCount()).isEqualTo(3); // 횟수는 증가
	}

	@Test
	@DisplayName("동일한 점수면 bestScore가 유지되고 횟수는 증가한다")
	void update_sameScore_keepsBestScore () {
		RankingBackup backup = RankingBackup.builder()
			.user(mock(com.aigo.speech.user.entity.User.class))
			.userUuid(java.util.UUID.randomUUID())
			.bestScore(70)
			.totalSessionCount(1)
			.build();

		backup.update(70);

		assertThat(backup.getBestScore()).isEqualTo(70);
		assertThat(backup.getTotalSessionCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("이번 주 첫 점수는 weeklyScore와 weekStartDate가 설정된다")
	void updateWeeklyScore_firstThisWeek_setsScore () {
		RankingBackup backup = RankingBackup.builder()
			.user(mock(com.aigo.speech.user.entity.User.class))
			.userUuid(java.util.UUID.randomUUID())
			.bestScore(0).totalSessionCount(0).build();

		backup.updateWeeklyScore(80);

		assertThat(backup.getWeeklyScore()).isEqualTo(80);
		assertThat(backup.getWeekStartDate()).isEqualTo(LocalDate.now().with(DayOfWeek.MONDAY));
	}

	@Test
	@DisplayName("이번 주 더 높은 점수가 오면 weeklyScore가 갱신된다")
	void updateWeeklyScore_higherScore_updates () {
		RankingBackup backup = RankingBackup.builder()
			.user(mock(com.aigo.speech.user.entity.User.class))
			.userUuid(java.util.UUID.randomUUID())
			.bestScore(0).totalSessionCount(0).build();

		backup.updateWeeklyScore(60);
		backup.updateWeeklyScore(85);

		assertThat(backup.getWeeklyScore()).isEqualTo(85);
	}

	@Test
	@DisplayName("이번 주 더 낮은 점수가 오면 weeklyScore가 유지된다")
	void updateWeeklyScore_lowerScore_keeps () {
		RankingBackup backup = RankingBackup.builder()
			.user(mock(com.aigo.speech.user.entity.User.class))
			.userUuid(java.util.UUID.randomUUID())
			.bestScore(0).totalSessionCount(0).build();

		backup.updateWeeklyScore(90);
		backup.updateWeeklyScore(70);

		assertThat(backup.getWeeklyScore()).isEqualTo(90);
	}
}