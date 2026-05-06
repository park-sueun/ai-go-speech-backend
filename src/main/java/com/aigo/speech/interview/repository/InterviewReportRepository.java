package com.aigo.speech.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;

public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {

	boolean existsBySession(InterviewSession session);

	Optional<InterviewReport> findBySession(InterviewSession session);

	List<InterviewReport> findBySessionIn(List<InterviewSession> sessions);
}
