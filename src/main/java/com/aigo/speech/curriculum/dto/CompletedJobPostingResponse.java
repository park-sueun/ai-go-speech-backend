package com.aigo.speech.curriculum.dto;

import java.util.UUID;

import com.aigo.speech.jobposting.entity.JobPosting;

public record CompletedJobPostingResponse(
	UUID jobPostingUuid,
	String companyName,
	String position,
	String site
) {
	public static CompletedJobPostingResponse from (JobPosting jobPosting) {
		return new CompletedJobPostingResponse(
			jobPosting.getUuid(),
			jobPosting.getCompanyName(),
			jobPosting.getPosition(),
			resolveSiteName(jobPosting.getUrl())
		);
	}

	private static String resolveSiteName (String url) {
		if (url.contains("jobkorea.co.kr")) return "잡코리아";
		if (url.contains("wanted.co.kr")) return "원티드";
		if (url.contains("saramin.co.kr")) return "사람인";
		return "기타";
	}
}
