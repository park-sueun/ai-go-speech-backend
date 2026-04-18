package com.aigo.speech.auth.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.Role;
import com.aigo.speech.user.entity.User;

class OAuthAttributesTest {

	@Test
	void ofGoogle_속성_파싱_성공 () {
		Map<String, Object> attributes = Map.of(
			"sub", "google-123",
			"email", "test@gmail.com",
			"name", "홍길동",
			"picture", "http://picture.url"
		);

		OAuthAttributes result = OAuthAttributes.of("google", attributes);

		assertThat(result.getProviderId()).isEqualTo("google-123");
		assertThat(result.getEmail()).isEqualTo("test@gmail.com");
		assertThat(result.getNickname()).isEqualTo("홍길동");
		assertThat(result.getProfileImage()).isEqualTo("http://picture.url");
		assertThat(result.getProvider()).isEqualTo(Provider.GOOGLE);
	}

	@Test
	void ofNaver_속성_파싱_성공 () {
		Map<String, Object> response = Map.of(
			"id", "naver-456",
			"email", "test@naver.com",
			"name", "네이버유저",
			"profile_image", "http://naver-picture.url"
		);
		Map<String, Object> attributes = Map.of("response", response);

		OAuthAttributes result = OAuthAttributes.of("naver", attributes);

		assertThat(result.getProviderId()).isEqualTo("naver-456");
		assertThat(result.getEmail()).isEqualTo("test@naver.com");
		assertThat(result.getNickname()).isEqualTo("네이버유저");
		assertThat(result.getProfileImage()).isEqualTo("http://naver-picture.url");
		assertThat(result.getProvider()).isEqualTo(Provider.NAVER);
	}

	@Test
	void ofKakao_속성_파싱_성공 () {
		Map<String, Object> profile = Map.of(
			"nickname", "카카오유저",
			"profile_image_url", "http://kakao-picture.url"
		);
		Map<String, Object> kakaoAccount = Map.of(
			"email", "test@kakao.com",
			"profile", profile
		);
		Map<String, Object> attributes = Map.of(
			"id", 789L,
			"kakao_account", kakaoAccount
		);

		OAuthAttributes result = OAuthAttributes.of("kakao", attributes);

		assertThat(result.getProviderId()).isEqualTo("789");
		assertThat(result.getEmail()).isEqualTo("test@kakao.com");
		assertThat(result.getNickname()).isEqualTo("카카오유저");
		assertThat(result.getProfileImage()).isEqualTo("http://kakao-picture.url");
		assertThat(result.getProvider()).isEqualTo(Provider.KAKAO);
	}

	@Test
	void of_대소문자_구분없이_파싱 () {
		Map<String, Object> attributes = Map.of(
			"sub", "google-123",
			"email", "test@gmail.com",
			"name", "홍길동",
			"picture", "http://picture.url"
		);

		assertThatNoException().isThrownBy(() -> OAuthAttributes.of("GOOGLE", attributes));
	}

	@Test
	void of_지원하지않는_provider_예외발생 () {
		assertThatThrownBy(() -> OAuthAttributes.of("github", Map.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Unsupported provider: github");
	}

	@Test
	void toEntity_User로_변환_성공 () {
		Map<String, Object> attributes = Map.of(
			"sub", "google-123",
			"email", "test@gmail.com",
			"name", "홍길동",
			"picture", "http://picture.url"
		);
		OAuthAttributes oAuthAttributes = OAuthAttributes.of("google", attributes);

		User user = oAuthAttributes.toEntity();

		assertThat(user.getProvider()).isEqualTo(Provider.GOOGLE);
		assertThat(user.getProviderId()).isEqualTo("google-123");
		assertThat(user.getEmail()).isEqualTo("test@gmail.com");
		assertThat(user.getNickname()).isEqualTo("홍길동");
		assertThat(user.getProfileImage()).isEqualTo("http://picture.url");
		assertThat(user.getRole()).isEqualTo(Role.USER);
	}
}
