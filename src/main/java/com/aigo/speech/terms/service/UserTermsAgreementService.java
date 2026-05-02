package com.aigo.speech.terms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.terms.dto.UserAgreementResponse;
import com.aigo.speech.terms.repository.UserTermsAgreementRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTermsAgreementService {

	private final UserTermsAgreementRepository userTermsAgreementRepository;
	private final UserRepository userRepository;

	public List<UserAgreementResponse> getUserAgreements (Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		return userTermsAgreementRepository.findAllByUser(user).stream()
			.map(UserAgreementResponse::from)
			.collect(Collectors.toList());
	}
}
