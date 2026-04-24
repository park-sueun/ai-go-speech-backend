package com.aigo.speech.interview.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateSessionRequest {

	private UUID jobPostingUuid;

	private boolean retry;

	private LocalDate interviewDate;

	@Valid
	@NotNull(message = "채용 공고 정보를 입력해주세요.")
	private JobPostingContext jobPostingContext;
}
