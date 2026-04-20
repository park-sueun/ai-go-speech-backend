package com.aigo.speech.user.controller;


import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.user.dto.UserDto.UpdateProfileRequest;
import com.aigo.speech.user.dto.UserDto.UserInfoResponse;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<UserInfoResponse>> getProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal String uuid
  ) {
    UserInfoResponse response = userService.getUserInfo(UUID.fromString(uuid));
    return ResponseEntity.ok(ApiResponse.success(response));
  }


  @PatchMapping("/profile/update")
  public ResponseEntity<ApiResponse<Void>> updateProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal String uuid,
      @RequestBody @Valid UpdateProfileRequest request
  ) {
    userService.updateProfile(UUID.fromString(uuid), request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @DeleteMapping("/delete")
  public ResponseEntity<ApiResponse<Void>> delete(
      @Parameter(hidden = true) @AuthenticationPrincipal String uuid
  ) {
    userService.withdraw(UUID.fromString(uuid));
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
