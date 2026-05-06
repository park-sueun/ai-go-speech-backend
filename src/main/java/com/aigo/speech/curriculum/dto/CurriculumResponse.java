package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.aigo.speech.curriculum.entity.Curriculum;

public record CurriculumResponse(
	UUID uuid,
	UUID jobPostingUuid,
	String content,
	LocalDate scheduledDate,
	boolean isPast
) {
	public static CurriculumResponse from (Curriculum curriculum) {
		LocalDate today = LocalDate.now();
		boolean isPast = curriculum.getScheduleDate().isBefore(today);

		UUID jobPostingUuid = (curriculum.getJobPosting() != null)
			? curriculum.getJobPosting().getUuid()
			: null;

		return new CurriculumResponse(
			curriculum.getUuid(),
			jobPostingUuid,
			curriculum.getContent().getTitle(),
			curriculum.getScheduleDate(),
			isPast
		);
	}
}
