package com.aigo.speech.answer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.user.entity.User;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

	Optional<InterviewAnswer> findByUuid(UUID uuid);

	boolean existsByQuestion(InterviewQuestion question);

	@Query("SELECT COUNT(a) FROM InterviewAnswer a WHERE a.question.session = :session")
	long countBySession(@Param("session") InterviewSession session);

	@Query("SELECT ia FROM InterviewAnswer ia JOIN FETCH ia.question q WHERE q.session = :session ORDER BY q.sequenceOrder")
	List<InterviewAnswer> findBySessionOrderBySequenceOrder(@Param("session") InterviewSession session);

	@Modifying
	@Query("DELETE FROM InterviewAnswer ia WHERE ia.question IN (SELECT iq FROM InterviewQuestion iq WHERE iq.session.user = :user)")
	void deleteAllBySessionUser(@Param("user") User user);
}
