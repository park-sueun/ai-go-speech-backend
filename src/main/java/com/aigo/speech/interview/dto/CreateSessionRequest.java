package com.aigo.speech.interview.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateSessionRequest {

	@NotNull(message = "채용 공고 UUID를 입력해주세요.")
	private UUID jobPostingUuid;

	private LocalDate interviewDate;
}
