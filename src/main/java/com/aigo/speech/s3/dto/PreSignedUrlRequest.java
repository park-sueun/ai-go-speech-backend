package com.aigo.speech.s3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PreSignedUrlRequest {
	@NotBlank
	@Pattern(
		regexp = "^(jpg|jpeg|png|webp)$",
		message = "허용된 확장자: jpg, jpeg, png, webp"
	)
	private String fileExtension;

	@NotBlank
	@Pattern(
		regexp = "^(image/jpeg|image/png|image/webp)$",
		message = "허용된 Content-Type: image/jpeg, image/png, image/webp"
	)
	private String contentType;
}
