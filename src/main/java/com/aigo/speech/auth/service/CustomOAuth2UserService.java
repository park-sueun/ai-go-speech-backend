package com.aigo.speech.auth.service;

import com.aigo.speech.auth.dto.OAuthAttributes;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

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

        User user = saveOrUpdate(attributes);

        Map<String, Object> userAttributes = Map.of(
            userNameAttributeName, attributes.getProviderId(),
            "email", user.getEmail(),
            "nickname", user.getNickname(),
            "role", user.getRole().name(),
            "userId", user.getId()
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

    private User saveOrUpdate (OAuthAttributes attributes) {
        return userRepository
            .findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
            .map(user -> {
                user.update(attributes.getNickname(), attributes.getProfileImage());
                return user;
            })
            .orElseGet(() -> userRepository.save(attributes.toEntity()));
    }
}
