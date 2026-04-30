package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.aigo.speech.curriculum.entity.Curriculum;

public record CurriculmResponse(
	UUID uuid,
	UUID JobPostingUuid,
	String content,
	LocalDate scheduledDate
) {
	public static CurriculmResponse from (Curriculum curriculum) {
		return new CurriculmResponse(
			curriculum.getUuid(),
			curriculum.getJobPosting().getUuid(),
			curriculum.getContent().getTitle(),
			curriculum.getScheduleDate()
		);
	}
}
