package com.aigo.speech.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewQuestion;
import com.aigo.speech.interview.entity.InterviewSession;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
	Optional<InterviewQuestion> findByUuid(UUID uuid);

	List<InterviewQuestion> findBySessionOrderBySequenceOrderAsc(InterviewSession session);

	long countBySession(InterviewSession session);
}
