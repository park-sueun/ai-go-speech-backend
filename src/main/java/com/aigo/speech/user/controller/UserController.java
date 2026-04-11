package com.aigo.speech.user.controller;

import com.aigo.speech.user.dto.LogoutRequest;
import com.aigo.speech.user.dto.TokenRequest;
import com.aigo.speech.user.dto.UserDto.LoginRequest;
import com.aigo.speech.user.dto.UserDto.SignupRequest;
import com.aigo.speech.user.dto.UserDto.TokenResponse;
import com.aigo.speech.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/jwt")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("/signup")
  public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
    userService.Signup(request);
    return ResponseEntity.ok("회원가입 성공");
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(userService.Login(request));
  }

  @PostMapping("/token/refresh")
  public ResponseEntity<TokenResponse> refresh(@RequestBody TokenRequest dto) {
    TokenResponse response = userService.updateRefreshToken(dto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<String> logout(@RequestBody LogoutRequest dto) {
    userService.logout(dto.email());
    return ResponseEntity.ok("로그아웃 성공");
  }
}
