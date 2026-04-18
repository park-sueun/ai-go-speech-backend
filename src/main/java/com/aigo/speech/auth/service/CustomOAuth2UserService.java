package com.aigo.speech.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.dto.OAuthAttributes;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.ProfileRepository;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;

	@Override
	@Transactional
	public OAuth2User loadUser (OAuth2UserRequest request) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = fetchOAuth2User(request);

		String registrationId = request.getClientRegistration().getRegistrationId();
		String userNameAttributeName = request.getClientRegistration()
			.getProviderDetails()
			.getUserInfoEndpoint()
			.getUserNameAttributeName();

		OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

		User user = saveOrUpdateUser(attributes);
		Profile profile = saveOrUpdateProfile(user, attributes);

		Map<String, Object> userAttributes = Map.of(
			userNameAttributeName, attributes.getProviderId(),
			"email", user.getEmail(),
			"nickname", profile.getNickname(),
			"role", user.getRole().name(),
			"userId", user.getUuid()
		);

		return new DefaultOAuth2User(
			List.of(new OAuth2UserAuthority("ROLE_" + user.getRole().name(), userAttributes)),
			userAttributes,
			userNameAttributeName
		);
	}

	protected OAuth2User fetchOAuth2User (OAuth2UserRequest request) {
		return super.loadUser(request);
	}

	private User saveOrUpdateUser (OAuthAttributes attributes) {
		return userRepository
			.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
			.orElseGet(() -> userRepository.save(attributes.toEntity()));
	}

	private Profile saveOrUpdateProfile (User user, OAuthAttributes attributes) {
		return profileRepository.findByUser(user)
			.map(profile -> {
				profile.update(attributes.getNickname(), attributes.getProfileImage());
				return profile;
			})
			.orElseGet(() -> profileRepository.save(
				Profile.builder()
					.user(user)
					.nickname(attributes.getNickname())
					.profileImageUrl(attributes.getProfileImage())
					.build()
			));
	}
	
}
