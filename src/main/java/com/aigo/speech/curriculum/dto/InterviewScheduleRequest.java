package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InterviewScheduleRequest(
	@NotBlank(message = "회사명은 필수입니다.")
	String companyName,
	@NotNull(message = "면접 날짜를 선택해주세요.")
	LocalDate interviewDate
) {
}
