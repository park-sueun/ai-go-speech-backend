package com.aigo.speech.interview.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewSession;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
	Optional<InterviewSession> findByUuid(UUID uuid);
}
