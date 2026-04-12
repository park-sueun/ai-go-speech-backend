package com.aigo.speech.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public class PasswordResetDto {
    @Getter
    public static class ForgotPasswordRequest {
        @Email
        @NotBlank
        private String email;
    }

    @Getter
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;

        @NotBlank
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        private String newPassword;
    }
}
