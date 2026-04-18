package com.aigo.speech.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(

	@NotBlank(message = "현재 비밀번호를 입력해주세요.")
	String currentPassword,

	@NotBlank(message = "새 비밀번호를 입력해주세요.")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
		message = "영문, 숫자, 특수문자를 포함하여 8자 이상 입력해주세요."
	)
	String newPassword,

	@NotBlank(message = "비밀번호 확인을 입력해주세요.")
	String confirmPassword
) {
}
