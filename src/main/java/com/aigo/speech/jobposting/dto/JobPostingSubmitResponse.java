package com.aigo.speech.jobposting.dto;

import java.util.UUID;

import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.entity.JobPostingStatus;

public record JobPostingSubmitResponse(
	UUID uuid,
	String url,
	JobPostingStatus status
) {
	public static JobPostingSubmitResponse from(JobPosting jp) {
		return new JobPostingSubmitResponse(jp.getUuid(), jp.getUrl(), jp.getStatus());
	}
}
