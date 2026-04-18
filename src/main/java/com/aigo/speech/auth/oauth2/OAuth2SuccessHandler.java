package com.aigo.speech.auth.oauth2;

import com.aigo.speech.auth.jwt.JwtTokenProvider;
import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess (
        HttpServletRequest request, HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        Provider provider = Provider.valueOf(
            oauthToken.getAuthorizedClientRegistrationId().toUpperCase());
        String providerId = oAuth2User.getName();

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        user.updateRefreshToken(refreshToken);

        // HttpOnly 쿠키로 JWT 전달 (URL 파라미터 노출 방지)
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(jwtTokenProvider.getAccessTokenExpiration() / 1000)
            .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        response.sendRedirect(baseUrl + "/oauth2/callback");
    }
}
