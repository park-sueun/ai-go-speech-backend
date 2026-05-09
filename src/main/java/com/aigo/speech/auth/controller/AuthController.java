package com.aigo.speech.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.auth.dto.AuthDto.LoginRequest;
import com.aigo.speech.auth.dto.AuthDto.SignupRequest;
import com.aigo.speech.auth.dto.AuthDto.TokenResponse;
import com.aigo.speech.auth.dto.ChangePasswordRequest;
import com.aigo.speech.auth.dto.EmailVerificationConfirmRequest;
import com.aigo.speech.auth.dto.EmailVerificationRequest;
import com.aigo.speech.auth.dto.PasswordResetDto;
import com.aigo.speech.auth.dto.TokenRequest;
import com.aigo.speech.auth.service.AuthService;
import com.aigo.speech.auth.service.EmailVerificationService;
import com.aigo.speech.auth.service.PasswordResetService;
import com.aigo.speech.global.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final EmailVerificationService emailVerificationService;
	private final PasswordResetService passwordResetService;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<String>> signup (@RequestBody @Valid SignupRequest request) {
		authService.signup(request);
		return ResponseEntity.ok(ApiResponse.success("회원가입 성공"));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login (@RequestBody LoginRequest request) {
		TokenResponse response = authService.login(request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh (@RequestBody TokenRequest dto) {
		TokenResponse response = authService.updateRefreshToken(dto);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 로그아웃
	 * - 자체 로그인: Authorization 헤더로 토큰 전달
	 * - 소셜 로그인: HttpOnly 쿠키로 토큰이 자동 전달됨
	 * JwtAuthenticationFilter가 둘 다 처리하므로 @AuthenticationPrincipal로 UUID를 가져옴
	 */
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout (
		@Parameter(hidden = true) @AuthenticationPrincipal String userUuid,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		authService.logout(UUID.fromString(userUuid));

		clearAuthCookies(response);

		return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
	}

	// 인증 코드 발송
	@PostMapping("/email-verifications")
	public ResponseEntity<ApiResponse<Void>> sendVerificationCode (
		@RequestBody @Valid EmailVerificationRequest request
	) {
		emailVerificationService.sendVerificationCode(request.email());
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	// 인증 코드 검증
	@PostMapping("/email-verifications/verify")
	public ResponseEntity<ApiResponse<Void>> verifyCode (
		@RequestBody @Valid EmailVerificationConfirmRequest request
	) {
		emailVerificationService.verifyCode(
			request.email(),
			request.code()
		);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	// 패스워드 초기화 메일 발송
	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword (
		@RequestBody @Valid PasswordResetDto.ForgotPasswordRequest request
	) {
		passwordResetService.sendPasswordResetEmail(request.getEmail());

		// 이메일 존재 여부 무관하게 동일 응답
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	// 패스워드 초기화
	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword (
		@RequestBody @Valid PasswordResetDto.ResetPasswordRequest request
	) {
		passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());

		return ResponseEntity.ok(ApiResponse.success(null));
	}

	// 패스워드 변경
	@PatchMapping("/password")
	public ResponseEntity<ApiResponse<Void>> changePassword (
		@Parameter(hidden = true) @AuthenticationPrincipal String uuid,
		@RequestBody @Valid ChangePasswordRequest request
	) {
		passwordResetService.changePassword(
			UUID.fromString(uuid),
			request.currentPassword(),
			request.newPassword(),
			request.confirmPassword()
		);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	private void clearAuthCookies (HttpServletResponse response) {
		ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
			.httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
		ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
			.httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
		response.addHeader("Set-Cookie", clearAccess.toString());
		response.addHeader("Set-Cookie", clearRefresh.toString());
	}

}
