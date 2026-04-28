package com.aigo.speech.user.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.auth.dto.EmailVerificationConfirmRequest;
import com.aigo.speech.auth.dto.EmailVerificationRequest;
import com.aigo.speech.auth.service.EmailVerificationService;
import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.user.dto.UserDto.UpdateProfileRequest;
import com.aigo.speech.user.dto.UserDto.UserInfoResponse;
import com.aigo.speech.user.service.UserService;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final EmailVerificationService emailVerificationService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserInfoResponse>> getProfile (
		@Parameter(hidden = true) @AuthenticationPrincipal String uuid
	) {
		UserInfoResponse response = userService.getUserInfo(UUID.fromString(uuid));
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PostMapping("/email-verifications") // 인증 코드 발송
	public ResponseEntity<ApiResponse<Void>> sendUpdateEmailCode (
		@RequestBody @Valid EmailVerificationRequest request
	) {
		userService.checkEmailDuplicate(request.email());
		emailVerificationService.sendVerificationCode(request.email());
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PostMapping("/email-verifications/verify") // 인증 코드 검증
	public ResponseEntity<ApiResponse<Void>> verifyEmailCode (
		@RequestBody @Valid EmailVerificationConfirmRequest request
	) {
		emailVerificationService.verifyCode(request.email(), request.code());
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PatchMapping("/me/profile") // 프로필 수정
	public ResponseEntity<ApiResponse<Void>> updateProfile (
		@Parameter(hidden = true) @AuthenticationPrincipal String uuid,
		@RequestBody @Valid UpdateProfileRequest request
	) {
		userService.updateProfile(UUID.fromString(uuid), request);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@GetMapping("/nickname-check")
	public ResponseEntity<ApiResponse<String>> checkNickname (@RequestParam String nickname) {
		userService.validateDuplicateNickname(nickname);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<Void>> delete (
		@Parameter(hidden = true) @AuthenticationPrincipal String uuid
	) {
		userService.deleteUser(UUID.fromString(uuid));
		return ResponseEntity.ok(ApiResponse.success(null));
	}
}
