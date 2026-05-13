package com.aigo.speech.s3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class PreSignedUrlResponse {
	private String preSignedUrl;
	private String s3Key;
	private String publicUrl;
}
