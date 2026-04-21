package com.aigo.speech.jobposting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JobPostingAnalyzeRequest {

	@NotBlank(message = "URL을 입력해주세요.")
	@Pattern(
		regexp = "^https?://.*",
		message = "올바른 URL 형식이 아닙니다."
	)
	private String url;
	
}
