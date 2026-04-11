package com.aigo.speech.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

  @Getter @Setter
  public static class SignupRequest{
    private String email;
    private String password;
    private String username;
  }
  @Getter @Setter
  public static class LoginRequest{
    private String email;
    private String password;
  }
  @Getter @AllArgsConstructor
  public static class TokenResponse{
    private String accessToken;
    private String refreshToken;
  }
}
