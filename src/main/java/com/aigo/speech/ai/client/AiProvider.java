package com.aigo.speech.ai.client;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;

public interface AiProvider {

	String getProvider();

	int getPriority();

	boolean isEnabled();

	String complete(String systemPrompt, String userPrompt);

	AiResponse complete(AiPromptRequest request, String renderedPrompt);

	/** "Retry-After: 60" 형식(초 단위)을 ms로 변환. 파싱 불가 시 -1 반환. */
	default long parseRetryAfterMs(String header) {
		if (header == null || header.isBlank())
			return -1;
		try {
			return Long.parseLong(header.trim()) * 1000L;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	default boolean isTimeout(Throwable e) {
		Throwable cause = e;
		while (cause != null) {
			if (cause instanceof java.util.concurrent.TimeoutException)
				return true;
			if (cause.getClass().getSimpleName().contains("TimeoutException"))
				return true;
			cause = cause.getCause();
		}
		return false;
	}
}
