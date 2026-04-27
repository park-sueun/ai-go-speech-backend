package com.aigo.speech.ai.dto;

public record AiResponse(
	String provider,
	String model,
	String content
) {
}
