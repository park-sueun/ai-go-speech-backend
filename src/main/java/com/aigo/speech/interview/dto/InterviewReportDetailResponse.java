package com.aigo.speech.interview.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.aigo.speech.answer.dto.AnswerAnalysisResponse;
import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.jobposting.entity.JobPosting;

public record InterviewReportDetailResponse(
	UUID sessionUuid,
	UUID jobPostingUuid,
	String companyName,
	String position,
	LocalDate interviewDate,
	LocalDateTime startedAt,
	LocalDateTime endedAt,
	Integer totalScore,
	Integer silenceScore,
	Integer fillerScore,
	Integer logicScore,
	String aiSummary,
	List<AnswerAnalysisResponse> analyses
) {
	public static InterviewReportDetailResponse from(
		InterviewSession session, InterviewReport report, List<AnswerAnalysis> analyses
	) {
		JobPosting jp = session.getJobPosting();
		return new InterviewReportDetailResponse(
			session.getUuid(),
			jp != null ? jp.getUuid() : null,
			jp != null ? jp.getCompanyName() : null,
			jp != null ? jp.getPosition() : null,
			session.getInterviewDate(),
			session.getStartedAt(),
			session.getEndedAt(),
			report.getTotalScore(),
			report.getSilenceScore(),
			report.getFillerScore(),
			report.getLogicScore(),
			report.getAiSummary(),
			analyses.stream().map(AnswerAnalysisResponse::from).toList()
		);
	}
}
