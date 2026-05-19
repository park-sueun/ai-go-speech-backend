package com.aigo.speech.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigo.speech.terms.repository.TermsRepository;
import com.aigo.speech.terms.repository.UserTermsAgreementRepository;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.Role;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.ProfileRepository;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

	@Mock UserRepository userRepository;
	@Mock ProfileRepository profileRepository;
	@Mock TermsRepository termsRepository;
	@Mock UserTermsAgreementRepository userTermsAgreementRepository;
	@Mock SocialUnlinkService socialUnlinkService;

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
	void setUp () {
		service = new CustomOAuth2UserService(
			userRepository, profileRepository, termsRepository, userTermsAgreementRepository, socialUnlinkService
		) {
			@Override
			protected OAuth2User fetchOAuth2User (OAuth2UserRequest request) {
				return new DefaultOAuth2User(
					List.of(new OAuth2UserAuthority(GOOGLE_ATTRIBUTES)),
					GOOGLE_ATTRIBUTES,
					"sub"
				);
			}
		};
	}

	private OAuth2UserRequest buildGoogleRequest () {
		OAuth2AccessToken token = new OAuth2AccessToken(
			OAuth2AccessToken.TokenType.BEARER,
			"token-value",
			Instant.now(),
			Instant.now().plusSeconds(3600)
		);
		return new OAuth2UserRequest(GOOGLE_CLIENT_REGISTRATION, token);
	}

	private User savedUser () {
		User user = User.builder()
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.email("test@gmail.com")
			.role(Role.USER)
			.build();
		ReflectionTestUtils.setField(user, "uuid", UUID.randomUUID());
		return user;
	}

	private Profile savedProfile (User user, String nickname) {
		return Profile.builder()
			.user(user)
			.nickname(nickname)
			.profileImageUrl("http://picture.url")
			.build();
	}

	// ======================== 기본 플로우 ========================

	@Test
	@DisplayName("신규 유저 저장 후 OAuth2User 반환")
	void loadUser_신규유저_저장후_OAuth2User_반환 () {
		User user = savedUser();

		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(user);
		given(termsRepository.findAllByIsActiveTrue()).willReturn(List.of());
		given(profileRepository.findByUser(user)).willReturn(Optional.empty());
		given(profileRepository.existsByNickname("홍길동")).willReturn(false);
		given(profileRepository.save(any(Profile.class))).willReturn(savedProfile(user, "홍길동"));

		OAuth2User result = service.loadUser(buildGoogleRequest());

		then(userRepository).should().save(any(User.class));
		then(profileRepository).should().save(any(Profile.class));
		assertThat((String)result.getAttribute("email")).isEqualTo("test@gmail.com");
		assertThat((String)result.getAttribute("nickname")).isEqualTo("홍길동");
		assertThat((String)result.getAttribute("role")).isEqualTo("USER");
	}

	@Test
	@DisplayName("기존 유저 재로그인 시 닉네임/이미지 유지, 약관 동의 저장 없음")
	void loadUser_기존유저_프로필_업데이트 () {
		User user = savedUser();
		Profile existingProfile = savedProfile(user, "구이름");
		ReflectionTestUtils.setField(existingProfile, "profileImageUrl", "http://old.url");

		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.of(user));
		given(profileRepository.findByUser(user)).willReturn(Optional.of(existingProfile));

		service.loadUser(buildGoogleRequest());

		then(userRepository).should(never()).save(any(User.class));
		then(termsRepository).should(never()).findAllByIsActiveTrue();
		assertThat(existingProfile.getNickname()).isEqualTo("구이름");
		assertThat(existingProfile.getProfileImageUrl()).isEqualTo("http://old.url");
	}

	@Test
	@DisplayName("반환된 OAuth2User authorities에 ROLE_ 포함")
	void loadUser_반환된_OAuth2User_authorities에_ROLE_포함 () {
		User user = savedUser();

		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(user);
		given(termsRepository.findAllByIsActiveTrue()).willReturn(List.of());
		given(profileRepository.findByUser(user)).willReturn(Optional.empty());
		given(profileRepository.existsByNickname("홍길동")).willReturn(false);
		given(profileRepository.save(any(Profile.class))).willReturn(savedProfile(user, "홍길동"));

		OAuth2User result = service.loadUser(buildGoogleRequest());

		assertThat(result.getAuthorities())
			.anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
	}

	// ======================== 이메일 중복 ========================

	@Test
	@DisplayName("신규 유저 가입 시 이메일 중복이면 OAuth2AuthenticationException 발생")
	void saveOrUpdateUser_이메일중복_예외발생 () {
		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.empty());
		given(userRepository.existsByEmail("test@gmail.com")).willReturn(true);

		assertThatThrownBy(() -> service.loadUser(buildGoogleRequest()))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.extracting(e -> ((OAuth2AuthenticationException)e).getError().getErrorCode())
			.isEqualTo("email_already_exists");

		then(userRepository).should(never()).save(any(User.class));
	}

	// ======================== 닉네임 중복 ========================

	@Test
	@DisplayName("닉네임 중복 없으면 원본 그대로 사용")
	void resolveNickname_중복없음_원본사용 () {
		User user = savedUser();

		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(user);
		given(termsRepository.findAllByIsActiveTrue()).willReturn(List.of());
		given(profileRepository.findByUser(user)).willReturn(Optional.empty());
		given(profileRepository.existsByNickname("홍길동")).willReturn(false);
		given(profileRepository.save(any(Profile.class))).willReturn(savedProfile(user, "홍길동"));

		service.loadUser(buildGoogleRequest());

		then(profileRepository).should().save(argThat(p -> p.getNickname().equals("홍길동")));
	}

	@Test
	@DisplayName("닉네임 1회 중복 시 닉네임_XXXX 형태로 저장")
	void resolveNickname_1회중복_접미사붙여_저장 () {
		User user = savedUser();

		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(user);
		given(termsRepository.findAllByIsActiveTrue()).willReturn(List.of());
		given(profileRepository.findByUser(user)).willReturn(Optional.empty());
		given(profileRepository.existsByNickname("홍길동")).willReturn(true);
		given(profileRepository.existsByNickname(argThat(n -> n.startsWith("홍길동_")))).willReturn(false);
		given(profileRepository.save(any(Profile.class))).willAnswer(inv -> inv.getArgument(0));

		service.loadUser(buildGoogleRequest());

		then(profileRepository).should().save(argThat(p ->
			p.getNickname().matches("홍길동_\\d{4}")
		));
	}

	@Test
	@DisplayName("닉네임 연속 중복 시 유니크 확정될 때까지 재시도")
	void resolveNickname_연속중복_유니크까지_재시도 () {
		User user = savedUser();

		given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(user);
		given(termsRepository.findAllByIsActiveTrue()).willReturn(List.of());
		given(profileRepository.findByUser(user)).willReturn(Optional.empty());
		// 원본 + 처음 3번의 후보 중복, 4번째부터 통과
		given(profileRepository.existsByNickname("홍길동")).willReturn(true);
		given(profileRepository.existsByNickname(argThat(n -> n.startsWith("홍길동_"))))
			.willReturn(true, true, true, false);
		given(profileRepository.save(any(Profile.class))).willAnswer(inv -> inv.getArgument(0));

		service.loadUser(buildGoogleRequest());

		then(profileRepository).should().save(argThat(p ->
			p.getNickname().matches("홍길동_\\d{4}")
		));
		// existsByNickname("홍길동_...") 최소 4번 호출 확인
		then(profileRepository).should(atLeast(4)).existsByNickname(argThat(n -> n.startsWith("홍길동_")));
	}
}
