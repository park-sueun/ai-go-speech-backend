package com.aigo.speech.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewReport.ReportStatus;
import com.aigo.speech.interview.entity.InterviewSession;

public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {

	boolean existsBySession(InterviewSession session);

	boolean existsBySessionAndStatusNot(InterviewSession session, ReportStatus status);

	Optional<InterviewReport> findBySession(InterviewSession session);

	List<InterviewReport> findBySessionIn(List<InterviewSession> sessions);
}
