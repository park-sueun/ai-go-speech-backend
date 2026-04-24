package com.aigo.speech.interview.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.interview.entity.InterviewAnswer;
import com.aigo.speech.interview.entity.InterviewSession;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
	Optional<InterviewAnswer> findByUuid(UUID uuid);

	@Query("SELECT COUNT(a) FROM InterviewAnswer a WHERE a.question.session = :session")
	long countBySession(@Param("session") InterviewSession session);
}
