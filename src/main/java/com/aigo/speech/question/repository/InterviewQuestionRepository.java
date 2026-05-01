package com.aigo.speech.question.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.question.entity.InterviewQuestion;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

	Optional<InterviewQuestion> findByUuid(UUID uuid);

	List<InterviewQuestion> findBySessionOrderBySequenceOrderAsc(InterviewSession session);

	Optional<InterviewQuestion> findBySessionAndSequenceOrder(InterviewSession session, int sequenceOrder);

	long countBySession(InterviewSession session);
}
