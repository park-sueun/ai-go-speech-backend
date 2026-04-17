package com.aigo.speech.auth.dto;

import java.util.Map;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.Role;
import com.aigo.speech.user.entity.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthAttributes {

	private String providerId;
	private String email;
	private String nickname;
	private String profileImage;
	private Provider provider;

	public static OAuthAttributes of (String registrationId, Map<String, Object> attributes) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> ofGoogle(attributes);
			case "naver" -> ofNaver(attributes);
			case "kakao" -> ofKakao(attributes);
			default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
		};
	}

	private static OAuthAttributes ofGoogle (Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.providerId((String)attributes.get("sub"))
			.email((String)attributes.get("email"))
			.nickname((String)attributes.get("name"))
			.profileImage((String)attributes.get("picture"))
			.provider(Provider.GOOGLE)
			.build();
	}

	@SuppressWarnings("unchecked")
	private static OAuthAttributes ofNaver (Map<String, Object> attributes) {
		Map<String, Object> response = (Map<String, Object>)attributes.get("response");
		return OAuthAttributes.builder()
			.providerId((String)response.get("id"))
			.email((String)response.get("email"))
			.nickname((String)response.get("name"))
			.profileImage((String)response.get("profile_image"))
			.provider(Provider.NAVER)
			.build();
	}

	@SuppressWarnings("unchecked")
	private static OAuthAttributes ofKakao (Map<String, Object> attributes) {
		Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");
		Map<String, Object> profile = (Map<String, Object>)kakaoAccount.get("profile");

		return OAuthAttributes.builder()
			.providerId(String.valueOf(attributes.get("id")))
			.email((String)kakaoAccount.get("email"))
			.nickname((String)profile.get("nickname"))
			.profileImage((String)profile.get("profile_image_url"))
			.provider(Provider.KAKAO)
			.build();
	}

	public User toEntity () {
		return User.builder()
			.provider(provider)
			.providerId(providerId)
			.email(email)
			.role(Role.USER)
			.build();
	}

}
