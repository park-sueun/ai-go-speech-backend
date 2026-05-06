package com.aigo.speech.curriculum.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.user.entity.User;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {
	Optional<InterviewSchedule> findByUuid (UUID uuid);

	List<InterviewSchedule> findByUserOrderByInterviewDateAsc (User user);
}
