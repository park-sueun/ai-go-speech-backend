package com.aigo.speech.curriculum.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.user.entity.User;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {
	Optional<InterviewSchedule> findByUuid (UUID uuid);

	List<InterviewSchedule> findByUserOrderByInterviewDateAsc (User user);

	List<InterviewSchedule> findByUserAndInterviewDateGreaterThanEqualOrderByInterviewDateAsc (User user, LocalDate from);

	boolean existsByJobPosting (JobPosting jobPosting);

	@Modifying
	void deleteAllByUser (User user);
}
