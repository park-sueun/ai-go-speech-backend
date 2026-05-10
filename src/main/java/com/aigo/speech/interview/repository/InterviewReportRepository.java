package com.aigo.speech.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.user.entity.User;

public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {

	boolean existsBySession(InterviewSession session);

	Optional<InterviewReport> findBySession(InterviewSession session);

	List<InterviewReport> findBySessionIn(List<InterviewSession> sessions);

	@Modifying
	@Query("DELETE FROM InterviewReport ir WHERE ir.session.user = :user")
	void deleteAllBySessionUser(@Param("user") User user);
}
