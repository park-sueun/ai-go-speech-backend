package com.aigo.speech.ai.dto;

import java.util.Map;

public record AiPromptRequest(
	String template,
	Map<String, String> variables,
	int maxTokens
) {
	public static AiPromptRequest of(String template, Map<String, String> variables) {
		return new AiPromptRequest(template, variables, 4096);
	}
}
