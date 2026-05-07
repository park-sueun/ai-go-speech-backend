package com.aigo.speech.ranking.exception;

public class RankingUnavailableException extends RuntimeException {
	public RankingUnavailableException (String message) {
		super(message);
	}

	public RankingUnavailableException (String message, Throwable cause) {
		super(message, cause);
	}
}
