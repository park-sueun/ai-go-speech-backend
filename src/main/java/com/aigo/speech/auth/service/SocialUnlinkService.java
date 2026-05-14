package com.aigo.speech.auth.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialUnlinkService {

	private static final String OAUTH_TOKEN_PREFIX = "oauth:token:";

	private final StringRedisTemplate redisTemplate;
	private final RestTemplate restTemplate;

	@Value("${oauth.naver.client-id}")
	private String naverClientId;

	@Value("${oauth.naver.client-secret}")
	private String naverClientSecret;

	@Value("${oauth.kakao.admin-key:}")
	private String kakaoAdminKey;

	public void saveOAuthToken (UUID userId, String accessToken, long ttlSeconds) {
		redisTemplate.opsForValue().set(OAUTH_TOKEN_PREFIX + userId, accessToken, ttlSeconds, TimeUnit.SECONDS);
	}

	public void revoke (User user) {
		if (user.getProvider() == null || user.getProvider() == Provider.LOCAL) {
			return;
		}
		try {
			switch (user.getProvider()) {
				case GOOGLE -> revokeGoogle(user);
				case NAVER -> revokeNaver(user);
				case KAKAO -> revokeKakao(user);
				default -> {
				}
			}
		} catch (Exception e) {
			log.warn("[SocialUnlink] {} 연동 해제 실패: userId={}, reason={}",
				user.getProvider(), user.getUuid(), e.getMessage());
		} finally {
			redisTemplate.delete(OAUTH_TOKEN_PREFIX + user.getUuid());
		}
	}

	private void revokeGoogle (User user) {
		String token = getToken(user.getUuid());
		if (token == null) {
			log.warn("[SocialUnlink] Google token not found, skip: {}", user.getUuid());
			return;
		}
		restTemplate.postForEntity("https://oauth2.googleapis.com/revoke?token=" + token, null, Void.class);
	}

	private void revokeNaver (User user) {
		String token = getToken(user.getUuid());
		if (token == null) {
			log.warn("[SocialUnlink] Naver token not found, skip: {}", user.getUuid());
			return;
		}
		String url = UriComponentsBuilder
			.fromUriString("https://nid.naver.com/oauth2.0/token")
			.queryParam("grant_type", "delete")
			.queryParam("client_id", naverClientId)
			.queryParam("client_secret", naverClientSecret)
			.queryParam("access_token", token)
			.toUriString();
		restTemplate.getForEntity(url, String.class);
	}

	private void revokeKakao (User user) {
		HttpHeaders headers = new HttpHeaders();
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

		if (StringUtils.hasText(kakaoAdminKey)) {
			headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
			body.add("target_id_type", "user_id");
			body.add("target_id", user.getProviderId());
		} else {
			String token = getToken(user.getUuid());
			if (token == null) {
				log.warn("[SocialUnlink] Kakao token not found and no admin key, skip: {}", user.getUuid());
				return;
			}
			headers.set("Authorization", "Bearer " + token);
		}

		restTemplate.postForEntity(
			"https://kapi.kakao.com/v1/user/unlink",
			new HttpEntity<>(body, headers),
			String.class
		);
	}

	private String getToken (UUID userId) {
		return redisTemplate.opsForValue().get(OAUTH_TOKEN_PREFIX + userId);
	}
}
