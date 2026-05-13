package com.aigo.speech.s3.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ConfirmProfileImageRequest {
	@NotBlank
	private String s3Key; // "profiles/1/uuid.jpg" 형태
}
