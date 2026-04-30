package com.aigo.speech.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.user.entity.User;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
	Optional<InterviewSession> findByUuid(UUID uuid);
	List<InterviewSession> findByUserOrderByCreatedAtDesc(User user);
}
