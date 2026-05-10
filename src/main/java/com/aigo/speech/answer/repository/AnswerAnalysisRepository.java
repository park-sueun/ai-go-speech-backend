package com.aigo.speech.answer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.user.entity.User;

public interface AnswerAnalysisRepository extends JpaRepository<AnswerAnalysis, Long> {

	@Query("SELECT COUNT(aa) FROM AnswerAnalysis aa WHERE aa.answer.question.session = :session")
	long countByAnswerQuestionSession(@Param("session") InterviewSession session);

	@Query("SELECT aa FROM AnswerAnalysis aa JOIN FETCH aa.answer a JOIN FETCH a.question q WHERE q.session = :session ORDER BY q.sequenceOrder")
	List<AnswerAnalysis> findBySessionWithDetails(@Param("session") InterviewSession session);

	@Query("SELECT aa FROM AnswerAnalysis aa JOIN FETCH aa.answer a JOIN FETCH a.question q JOIN FETCH q.session s JOIN FETCH s.user WHERE aa.uuid = :uuid")
	Optional<AnswerAnalysis> findByUuid(@Param("uuid") UUID uuid);

	@Modifying
	@Query("DELETE FROM AnswerAnalysis aa WHERE aa.answer IN (SELECT ia FROM InterviewAnswer ia WHERE ia.question IN (SELECT iq FROM InterviewQuestion iq WHERE iq.session.user = :user))")
	void deleteAllBySessionUser(@Param("user") User user);
}
