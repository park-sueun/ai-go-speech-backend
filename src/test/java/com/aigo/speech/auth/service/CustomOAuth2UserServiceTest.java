package com.aigo.speech.auth.service;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.Role;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    UserRepository userRepository;

    CustomOAuth2UserService service;

    private static final Map<String, Object> GOOGLE_ATTRIBUTES = Map.of(
            "sub", "google-123",
            "email", "test@gmail.com",
            "name", "홍길동",
            "picture", "http://picture.url"
    );

    private static final ClientRegistration GOOGLE_CLIENT_REGISTRATION = ClientRegistration
            .withRegistrationId("google")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("client-id")
            .redirectUri("http://localhost/callback")
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .build();

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(userRepository) {
            @Override
            protected OAuth2User fetchOAuth2User(OAuth2UserRequest request) {
                return new DefaultOAuth2User(
                        List.of(new OAuth2UserAuthority(GOOGLE_ATTRIBUTES)),
                        GOOGLE_ATTRIBUTES,
                        "sub"
                );
            }
        };
    }

    private OAuth2UserRequest buildGoogleRequest() {
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        return new OAuth2UserRequest(GOOGLE_CLIENT_REGISTRATION, token);
    }

    @Test
    void loadUser_신규유저_저장후_OAuth2User_반환() {
        User savedUser = User.builder()
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("홍길동")
                .profileImage("http://picture.url")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(savedUser, "uuid", UUID.randomUUID());

        given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        OAuth2User result = service.loadUser(buildGoogleRequest());

        then(userRepository).should().save(any(User.class));
        assertThat((String) result.getAttribute("email")).isEqualTo("test@gmail.com");
        assertThat((String) result.getAttribute("nickname")).isEqualTo("홍길동");
        assertThat((String) result.getAttribute("role")).isEqualTo("USER");
    }

    @Test
    void loadUser_기존유저_닉네임과_프로필이미지_업데이트() {
        User existingUser = User.builder()
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("구이름")
                .profileImage("http://old-picture.url")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(existingUser, "uuid", UUID.randomUUID());

        given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
                .willReturn(Optional.of(existingUser));

        service.loadUser(buildGoogleRequest());

        then(userRepository).should(never()).save(any(User.class));
        assertThat(existingUser.getNickname()).isEqualTo("홍길동");
        assertThat(existingUser.getProfileImage()).isEqualTo("http://picture.url");
    }

    @Test
    void loadUser_반환된_OAuth2User_authorities에_ROLE_포함() {
        User savedUser = User.builder()
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("홍길동")
                .profileImage("http://picture.url")
                .role(Role.USER)
                .build();

        ReflectionTestUtils.setField(savedUser, "uuid", UUID.randomUUID());
        given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        OAuth2User result = service.loadUser(buildGoogleRequest());

        assertThat(result.getAuthorities())
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"));
    }
}
