package com.aigo.speech.user.dto;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;


public class UserDto {

  @Getter
  @Builder
  public static class UserInfoResponse{
    private String email;
    private String nickname;
    private Provider provider;
    private String profileImageUrl;

    public static UserInfoResponse from(User user) { // 사용자 정보 조회
      return UserInfoResponse.builder()
          .email(user.getEmail())
          .nickname(user.getProfile().getNickname())
          .provider(user.getProvider())
          .profileImageUrl(user.getProfile().getProfileImageUrl())
          .build();
    }
  }

  @Getter
  public static class UpdateProfileRequest { // 사용자 정보 수정

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 2, max = 10, message = "2자 이상 10자 이하로 작성해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]*$", message = "특수문자는 입력할 수 없습니다.")
    private String nickname;

    private String profileImageUrl;
  }

  @Getter
  public static class WithdrawRequest { // 회원 탈퇴
    private String password;
  }
}
