package com.aigo.speech.ranking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.ranking.entity.RankingBackup;
import com.aigo.speech.user.entity.User;

public interface RankingBackupRepository extends JpaRepository<RankingBackup, Long> {
	Optional<RankingBackup> findByUserUuid (UUID userUuid);

	List<RankingBackup> findAllByUserUuidIn (List<UUID> userUuids);

	/* 최고 점수 기준 전체 목록 조회 */
	@Query("""
		SELECT r FROM RankingBackup r
		JOIN FETCH r.user u
		JOIN FETCH u.profile p
		ORDER BY r.bestScore DESC
		""")
	List<RankingBackup> findAllOrderByBestScoreDesc ();

	/* 특정 사용자보다 높은 인원수 계산 - 순위 계산 */
	@Query("""
		SELECT COUNT(r) FROM RankingBackup r
		WHERE r.bestScore > (
		    SELECT me.bestScore FROM RankingBackup me WHERE me.userUuid = :userUuid
		)
		""")
	long countHigherThan (@Param("userUuid") UUID userUuid);

	/* 이번 주 상위 N명 조회 */
	@Query("""
		SELECT r FROM RankingBackup r
		JOIN FETCH r.user u
		JOIN FETCH u.profile p
		WHERE r.weekStartDate = :weekStart
		ORDER BY r.weeklyScore DESC
		""")
	List<RankingBackup> findWeeklyTopOrderByWeeklyScoreDesc (@Param("weekStart") LocalDate weekStart);

	/* 이번 주 나보다 점수 높은 인원 수 */
	@Query("""
		SELECT COUNT(r) FROM RankingBackup r
		WHERE r.weekStartDate = :weekStart
		AND r.weeklyScore > :score
		""")
	long countHigherWeeklyScore (@Param("weekStart") LocalDate weekStart, @Param("score") int score);

	@Modifying
	void deleteByUser(User user);

}

