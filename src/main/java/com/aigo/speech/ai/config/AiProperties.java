package com.aigo.speech.ai.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
	String provider,
	ProviderProperties claude,
	ProviderProperties gemini,
	RetryProperties retry,
	TimeoutProperties timeout
) {
	public record ProviderProperties(
		String apiKey,
		String apiUrl,
		List<String> models,
		int priority,
		boolean enabled
	) {
	}

	public record RetryProperties(int maxAttempts, long delayMs, long jitterMs) {
	}

	public record TimeoutProperties(int connectMs, int readMs) {
	}
}
