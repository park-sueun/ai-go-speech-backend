package com.aigo.speech.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

	@Getter
	@Setter
	public static class SignupRequest {

		@NotBlank(message = "이메일을 입력해주세요.")
		@Email(message = "올바른 이메일 형식이 아닙니다.")
		private String email;

		@NotBlank(message = "비밀번호를 입력해주세요.")
		@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
			message = "영문, 숫자, 특수문자를 포함하여 8자 이상 입력해주세요."
		)
		private String password;

		@NotBlank(message = "비밀번호 확인을 입력해주세요.")
		private String confirmPassword;

		@NotBlank(message = "닉네임을 입력해주세요.")
		@Pattern(
			regexp = "^[가-힣a-zA-Z0-9]{1,20}$",
			message = "특수문자가 포함되어 있습니다."
		)
		private String nickname;
	}

	@Getter
	@Setter
	public static class LoginRequest {
		private String email;
		private String password;
	}

	@Getter
	@AllArgsConstructor
	public static class TokenResponse {
		private String accessToken;
		private String refreshToken;
	}
}
