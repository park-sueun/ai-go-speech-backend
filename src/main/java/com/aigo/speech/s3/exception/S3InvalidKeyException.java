package com.aigo.speech.s3.exception;

public class S3InvalidKeyException extends IllegalArgumentException {
	public S3InvalidKeyException (String message) {
		super(message);
	}
}
