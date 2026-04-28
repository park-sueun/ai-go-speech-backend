package com.aigo.speech.jobposting.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.entity.JobPostingStatus;

public record JobPostingDetailResponse(
	UUID uuid,
	String url,
	JobPostingStatus status,
	String companyName,
	String companyDescription,
	String position,
	List<String> responsibilities,
	List<String> requiredSkills,
	List<String> preferredSkills,
	String requiredExperience,
	String failureReason,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static JobPostingDetailResponse from(JobPosting jp) {
		return new JobPostingDetailResponse(
			jp.getUuid(),
			jp.getUrl(),
			jp.getStatus(),
			jp.getCompanyName(),
			jp.getCompanyDescription(),
			jp.getPosition(),
			jp.getResponsibilities(),
			jp.getRequiredSkills(),
			jp.getPreferredSkills(),
			jp.getRequiredExperience(),
			jp.getFailureReason(),
			jp.getCreatedAt(),
			jp.getUpdatedAt()
		);
	}
}
