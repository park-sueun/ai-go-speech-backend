package com.aigo.speech.ai.exception;

public class AiTimeoutException extends AiException {

	public AiTimeoutException(String provider) {
		super(provider + " API 응답 시간이 초과되었습니다.");
	}
}
