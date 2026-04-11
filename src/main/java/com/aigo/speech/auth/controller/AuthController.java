package com.aigo.speech.auth.controller;

import com.aigo.speech.auth.dto.AuthDto.LoginRequest;
import com.aigo.speech.auth.dto.AuthDto.SignupRequest;
import com.aigo.speech.auth.dto.AuthDto.TokenResponse;
import com.aigo.speech.auth.dto.TokenRequest;
import com.aigo.speech.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

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
}
