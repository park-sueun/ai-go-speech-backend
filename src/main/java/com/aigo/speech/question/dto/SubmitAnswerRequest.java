package com.aigo.speech.question.dto;

import java.util.List;

import com.aigo.speech.global.dto.TimePeriod;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubmitAnswerRequest {

	@NotNull(message = "총 경과 시간을 입력해주세요.")
	private Integer totalElapsedMs;

	private String transcript;

	private List<String> fillerWords;

	private List<TimePeriod> silencePeriods;

	private List<TimePeriod> speechPeriods;
}
