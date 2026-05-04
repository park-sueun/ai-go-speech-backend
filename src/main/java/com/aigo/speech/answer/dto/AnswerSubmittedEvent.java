package com.aigo.speech.answer.dto;

public record AnswerSubmittedEvent(
	Long answerId,
	Integer silenceCount,
	Integer totalSilenceDuration
) {
}
