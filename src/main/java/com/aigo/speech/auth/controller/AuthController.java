package com.aigo.speech.auth.controller;

import com.aigo.speech.auth.dto.EmailVerificationConfirmRequest;
import com.aigo.speech.auth.dto.EmailVerificationRequest;
import com.aigo.speech.auth.dto.PasswordResetDto;
import com.aigo.speech.auth.service.EmailVerificationService;
import com.aigo.speech.auth.service.PasswordResetService;
import com.aigo.speech.global.dto.ApiResponse;
import jakarta.validation.Valid;
import com.aigo.speech.auth.dto.AuthDto.LoginRequest;
import com.aigo.speech.auth.dto.AuthDto.SignupRequest;
import com.aigo.speech.auth.dto.AuthDto.TokenResponse;
import com.aigo.speech.auth.dto.TokenRequest;
import com.aigo.speech.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final EmailVerificationService emailVerificationService;
  private final PasswordResetService passwordResetService;

  @PostMapping("/signup")
  public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
    authService.signup(request);
    return ResponseEntity.ok("회원가입 성공");
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@RequestBody TokenRequest dto) {
    TokenResponse response = authService.updateRefreshToken(dto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<String> logout(@RequestHeader(value = "Authorization") String bearerToken) { // 토큰으로 로그아웃

    if(!bearerToken.startsWith("Bearer ")) {
      return ResponseEntity.badRequest().body("유효하지 않은 인증 헤더입니다.");
    }

    String accessToken = bearerToken.substring(7);
    authService.logout(accessToken);
    return ResponseEntity.ok("로그아웃 성공");
  }

    // 인증 코드 발송
    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @RequestBody @Valid EmailVerificationRequest request
    ) {
        emailVerificationService.sendVerificationCode(request.email());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 인증 코드 검증
    @PostMapping("/email-verifications/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
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
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody @Valid PasswordResetDto.ForgotPasswordRequest request
    ) {
        passwordResetService.sendPasswordResetEmail(request.getEmail());
        
        // 이메일 존재 여부 무관하게 동일 응답
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 패스워드 초기화
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid PasswordResetDto.ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}