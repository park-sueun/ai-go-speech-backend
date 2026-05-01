package com.aigo.speech.curriculum.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.curriculum.entity.InterviewSchedule;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {
	Optional<InterviewSchedule> findByUuid (UUID uuid);
}
