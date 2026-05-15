package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record InterviewScheduleUpdateRequest(
	@NotNull(message = "면접 날짜를 선택해주세요.")
	LocalDate interviewDate
) {
}

