package com.aigo.speech.answer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.interview.entity.InterviewSession;

public interface AnswerAnalysisRepository extends JpaRepository<AnswerAnalysis, Long> {

	@Query("SELECT COUNT(aa) FROM AnswerAnalysis aa WHERE aa.answer.question.session = :session")
	long countByAnswerQuestionSession(@Param("session") InterviewSession session);

	@Query("SELECT aa FROM AnswerAnalysis aa JOIN FETCH aa.answer a JOIN FETCH a.question q WHERE q.session = :session ORDER BY q.sequenceOrder")
	List<AnswerAnalysis> findBySessionWithDetails(@Param("session") InterviewSession session);
}
