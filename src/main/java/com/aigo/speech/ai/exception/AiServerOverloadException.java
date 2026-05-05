package com.aigo.speech.ai.exception;

public class AiServerOverloadException extends AiException {

	public AiServerOverloadException(String provider) {
		super(provider + " 서버 과부하 상태입니다. 다른 공급자로 전환합니다.");
	}
}
