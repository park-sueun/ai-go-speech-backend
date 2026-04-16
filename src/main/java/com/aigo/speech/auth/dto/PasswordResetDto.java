package com.aigo.speech.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

public class PasswordResetDto {

	@Getter
	public static class ForgotPasswordRequest {
		@NotBlank(message = "이메일을 입력해주세요.")
		@Email(message = "올바른 이메일 형식이 아닙니다.")
		private String email;
	}

	@Getter
	public static class ResetPasswordRequest {

		@NotBlank(message = "토큰이 없습니다.")
		private String token;

		@NotBlank(message = "새 비밀번호를 입력해주세요.")
		@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
			message = "비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 포함한 8자 이상이어야 합니다."
		)
		private String newPassword;

		@NotBlank(message = "비밀번호 확인을 입력해주세요.")
		private String confirmPassword;
	}

}
