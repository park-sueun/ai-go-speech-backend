package com.aigo.speech.ai.exception;

public class AiRateLimitException extends AiException {

	private final long retryAfterMs;

	public AiRateLimitException(String provider) {
		super(provider + " API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
		this.retryAfterMs = -1;
	}

	public AiRateLimitException(String provider, long retryAfterMs) {
		super(provider + " API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
		this.retryAfterMs = retryAfterMs;
	}

	/** Retry-After 헤더 기반 대기 시간(ms). 헤더 없으면 -1. */
	public long getRetryAfterMs() {
		return retryAfterMs;
	}
}
