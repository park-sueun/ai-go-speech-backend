package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record InterviewScheduleFromJobPostingRequest(
	@NotNull(message = "채용 공고 UUID는 필수입니다.")
	UUID jobPostingUuid,
	@NotNull(message = "면접 날짜를 선택해주세요.")
	LocalDate interviewDate
) {
}
