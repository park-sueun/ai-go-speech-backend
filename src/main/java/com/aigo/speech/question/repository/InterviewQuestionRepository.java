package com.aigo.speech.question.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.user.entity.User;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

	Optional<InterviewQuestion> findByUuid(UUID uuid);

	List<InterviewQuestion> findBySessionOrderBySequenceOrderAsc(InterviewSession session);

	Optional<InterviewQuestion> findBySessionAndSequenceOrder(InterviewSession session, int sequenceOrder);

	long countBySession(InterviewSession session);

	@Modifying
	@Query("DELETE FROM InterviewQuestion iq WHERE iq.session.user = :user")
	void deleteAllBySessionUser(@Param("user") User user);
}
