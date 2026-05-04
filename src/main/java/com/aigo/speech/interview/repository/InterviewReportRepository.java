package com.aigo.speech.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;

public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {

	boolean existsBySession(InterviewSession session);
}
