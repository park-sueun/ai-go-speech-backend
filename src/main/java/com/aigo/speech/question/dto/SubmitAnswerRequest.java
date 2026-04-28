package com.aigo.speech.question.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubmitAnswerRequest {

	@NotBlank(message = "오디오 URL을 입력해주세요.")
	private String audioUrl;

	private String sttText;

	private Integer duration;

	private String silenceIntervalsJson;

	private LocalDateTime answerStartedAt;

	private LocalDateTime answerEndedAt;

	private Integer silenceCount;

	private Integer totalSilenceDuration;

	private Integer answerDuration;
}
