package com.aigo.speech.ai.config;

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
		String model,
		int priority,
		boolean enabled
	) {
	}

	public record RetryProperties(int maxAttempts, long delayMs) {
	}

	public record TimeoutProperties(int connectMs, int readMs) {
	}
}
