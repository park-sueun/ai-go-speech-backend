package com.aigo.speech.user.service;


import com.aigo.speech.global.config.JwtTokenProvider;
import com.aigo.speech.user.dto.TokenRequest;
import com.aigo.speech.user.dto.UserDto.LoginRequest;
import com.aigo.speech.user.dto.UserDto.SignupRequest;
import com.aigo.speech.user.dto.UserDto.TokenResponse;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  @Transactional
  public void signup(SignupRequest dto){ // 회원가입
    if(userRepository.existsByEmail(dto.getEmail())){
      throw new RuntimeException("이미 존재하는 이메일입니다.");
    }
    userRepository.save(User.builder()
        .email(dto.getEmail())
        .password(bCryptPasswordEncoder.encode(dto.getPassword()))
        .username(dto.getUsername())
        .build());
  }

  @Transactional
  public TokenResponse login(LoginRequest dto){
    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
    if(!bCryptPasswordEncoder.matches(dto.getPassword(), user.getPassword())){
      throw new RuntimeException("비밀번호가 일치하지 않습니다.");
    }

    String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    user.updateRefreshToken(refreshToken); // DB에 refreshToken 저장

    return new TokenResponse(accessToken, refreshToken);
  }

  // 토큰 갱신
  @Transactional
  public TokenResponse updateRefreshToken(TokenRequest dto){
    String refreshToken = dto.refreshToken();
    if(!jwtTokenProvider.validateToken(refreshToken)){
      throw new RuntimeException("리프레시 토큰이 유효하지 않습니다.");
    }

    String email = jwtTokenProvider.getEmail(refreshToken);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
      throw new RuntimeException("일치하는 토큰 정보가 없습니다. 다시 시도해주세요.");
    }

    String newAccessToken = jwtTokenProvider.createAccessToken(email);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

    user.updateRefreshToken(newRefreshToken);

    return new TokenResponse(newAccessToken, newRefreshToken);
  }

  @Transactional
  public void logout(String accessToken) {
    String email = jwtTokenProvider.getEmail(accessToken);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
    user.updateRefreshToken(null); // 세션 만료
  }
}
