package com.aigo.speech.ai.exception;

public class AiRateLimitException extends AiException {

	public AiRateLimitException(String provider) {
		super(provider + " API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
	}
}
