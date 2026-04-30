package com.aigo.speech.curriculum.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.jobposting.entity.JobPosting;

public interface CurriculmRepository extends JpaRepository<Curriculum, Long> {

	Optional<Curriculum> findByUuid (UUID uuid);

	// 면접 일정에 연결된 커리큘럼 조회(가까운 일정순)
	List<Curriculum> findByJobPostingOrderByScheduleDateAsc (JobPosting jobPosting);

	// 특정 사용자의 커리큘럼 조회(가까운 일정순)
	List<Curriculum> findByUserIdOrderByScheduleDateAsc (Long userId);

	//오늘 할 일
	List<Curriculum> findByUserIdAndScheduleDate (Long UserId, LocalDate scheduleDate);

	boolean existsByJobPosting (JobPosting jobPosting);

}
