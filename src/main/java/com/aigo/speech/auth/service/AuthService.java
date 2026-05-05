package com.aigo.speech.auth.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.dto.AuthDto.LoginRequest;
import com.aigo.speech.auth.dto.AuthDto.SignupRequest;
import com.aigo.speech.auth.dto.AuthDto.TokenResponse;
import com.aigo.speech.auth.dto.TokenRequest;
import com.aigo.speech.auth.exception.DuplicateEmailException;
import com.aigo.speech.auth.exception.DuplicateNicknameException;
import com.aigo.speech.auth.exception.InvalidCredentialsException;
import com.aigo.speech.auth.exception.InvalidTokenException;
import com.aigo.speech.auth.exception.PasswordMismatchException;
import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.auth.jwt.JwtTokenProvider;
import com.aigo.speech.terms.entity.Terms;
import com.aigo.speech.terms.entity.UserTermsAgreement;
import com.aigo.speech.terms.exception.InvalidTermsAgreementException;
import com.aigo.speech.terms.repository.TermsRepository;
import com.aigo.speech.terms.repository.UserTermsAgreementRepository;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.Role;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.ProfileRepository;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final TermsRepository termsRepository;
	private final UserTermsAgreementRepository userTermsAgreementRepository;

	@Transactional
	public void signup (SignupRequest dto) { // 회원가입

		if (!dto.getPassword().equals(dto.getConfirmPassword())) {
			throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
		}

		if (userRepository.existsByEmail(dto.getEmail())) {
			throw new DuplicateEmailException("이미 사용중인 이메일입니다.");
		}
		if (profileRepository.existsByNickname(dto.getNickname())) {
			throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
		}

		validateRequiredTerms(dto.getAgreedTerms());

		User user = userRepository.save(User.builder()
			.email(dto.getEmail())
			.password(bCryptPasswordEncoder.encode(dto.getPassword()))
			.provider(Provider.LOCAL)
			.role(Role.USER)
			.build());

		profileRepository.save(Profile.builder()
			.user(user)
			.nickname(dto.getNickname())
			.build());

		if (dto.getAgreedTerms() != null && !dto.getAgreedTerms().isEmpty()) {
			List<Terms> agreedTerms = termsRepository.findAllByUuidIn(dto.getAgreedTerms());
			List<UserTermsAgreement> agreeTerms = agreedTerms.stream()
				.map(terms -> new UserTermsAgreement(user, terms))
				.collect(Collectors.toList());

			userTermsAgreementRepository.saveAll(agreeTerms);

		}
	}

	private void validateRequiredTerms (List<UUID> agreedTerms) {
		List<UUID> requiredTerms = termsRepository.findAllByIsActiveTrue().stream()
			.filter(Terms::getRequired)
			.map(Terms::getUuid)
			.collect((Collectors.toList()));

		Set<UUID> agreedTermsSet = (agreedTerms == null) ? new HashSet<>() : new HashSet<>(agreedTerms);

		if (!agreedTermsSet.containsAll(requiredTerms)) {
			throw new InvalidTermsAgreementException("모든 필수 약관에 동의해야 합니다.");
		}
	}

	@Transactional
	public TokenResponse login (LoginRequest dto) {

		User user = userRepository.findByEmail(dto.getEmail())
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		if (!bCryptPasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
			throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
		}

		String accessToken = jwtTokenProvider.createAccessToken(user.getUuid());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUuid());

		user.updateRefreshToken(refreshToken); // DB에 refreshToken 저장

		return new TokenResponse(accessToken, refreshToken);
	}

	// 토큰 갱신
	@Transactional
	public TokenResponse updateRefreshToken (TokenRequest dto) {

		String refreshToken = dto.refreshToken();

		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new InvalidTokenException("리프레시 토큰이 유효하지 않습니다.");
		}

		UUID uuid = jwtTokenProvider.getUuid(refreshToken);

		User user = userRepository.findByUuid(uuid)
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
			throw new InvalidTokenException("일치하는 토큰 정보가 없습니다. 다시 시도해주세요.");
		}

		String newAccessToken = jwtTokenProvider.createAccessToken(uuid);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(uuid);

		user.updateRefreshToken(newRefreshToken);

		return new TokenResponse(newAccessToken, newRefreshToken);
	}

	@Transactional
	public void logout (String accessToken) {

		UUID uuid = jwtTokenProvider.getUuid(accessToken);

		User user = userRepository.findByUuid(uuid)
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		user.updateRefreshToken(null); // 세션 만료
	}

}
