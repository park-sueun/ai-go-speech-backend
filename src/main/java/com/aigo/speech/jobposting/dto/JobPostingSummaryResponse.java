package com.aigo.speech.jobposting.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.entity.JobPostingStatus;

public record JobPostingSummaryResponse(
	UUID uuid,
	String url,
	String companyName,
	String position,
	JobPostingStatus status,
	LocalDateTime createdAt
) {
	public static JobPostingSummaryResponse from(JobPosting jp) {
		return new JobPostingSummaryResponse(
			jp.getUuid(),
			jp.getUrl(),
			jp.getCompanyName(),
			jp.getPosition(),
			jp.getStatus(),
			jp.getCreatedAt()
		);
	}
}
