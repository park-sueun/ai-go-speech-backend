package com.aigo.speech.jobposting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JobPostingUpdateRequest {

	@NotBlank(message = "회사명을 입력해주세요.")
	private String companyName;
}
