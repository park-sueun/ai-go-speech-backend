package com.aigo.speech.interview.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.fasterxml.jackson.annotation.JsonInclude;

public record InterviewReportSummaryResponse(
	UUID sessionUuid,
	String companyName,
	String position,
	LocalDate interviewDate,
	LocalDateTime startedAt,
	LocalDateTime endedAt,
	long questionCount,
	boolean analysisComplete,
	@JsonInclude(JsonInclude.Include.NON_NULL) Integer totalScore,
	@JsonInclude(JsonInclude.Include.NON_NULL) Integer silenceScore,
	@JsonInclude(JsonInclude.Include.NON_NULL) Integer fillerScore,
	@JsonInclude(JsonInclude.Include.NON_NULL) Integer logicScore
) {
	public static InterviewReportSummaryResponse from(
		InterviewSession session, InterviewReport report, long questionCount
	) {
		JobPosting jp = session.getJobPosting();
		boolean complete = report != null;
		return new InterviewReportSummaryResponse(
			session.getUuid(),
			jp != null ? jp.getCompanyName() : null,
			jp != null ? jp.getPosition() : null,
			session.getInterviewDate(),
			session.getStartedAt(),
			session.getEndedAt(),
			questionCount,
			complete,
			complete ? report.getTotalScore() : null,
			complete ? report.getSilenceScore() : null,
			complete ? report.getFillerScore() : null,
			complete ? report.getLogicScore() : null
		);
	}
}
