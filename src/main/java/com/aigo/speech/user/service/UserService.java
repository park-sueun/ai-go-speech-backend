package com.aigo.speech.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.answer.repository.AnswerAnalysisRepository;
import com.aigo.speech.answer.repository.InterviewAnswerRepository;
import com.aigo.speech.auth.exception.DuplicateEmailException;
import com.aigo.speech.auth.exception.DuplicateNicknameException;
import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.auth.service.EmailVerificationService;
import com.aigo.speech.curriculum.repository.CurriculumRepository;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
import com.aigo.speech.mail.exception.MailVerificationException;
import com.aigo.speech.question.repository.InterviewQuestionRepository;
import com.aigo.speech.ranking.repository.RankingBackupRepository;
import com.aigo.speech.terms.repository.UserTermsAgreementRepository;
import com.aigo.speech.user.dto.UserDto.UpdateProfileRequest;
import com.aigo.speech.user.dto.UserDto.UserInfoResponse;
import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.ProfileRepository;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final EmailVerificationService emailVerificationService;
	private final AnswerAnalysisRepository answerAnalysisRepository;
	private final InterviewAnswerRepository interviewAnswerRepository;
	private final InterviewReportRepository interviewReportRepository;
	private final InterviewQuestionRepository interviewQuestionRepository;
	private final InterviewSessionRepository interviewSessionRepository;
	private final RankingBackupRepository rankingBackupRepository;
	private final CurriculumRepository curriculumRepository;
	private final InterviewScheduleRepository interviewScheduleRepository;
	private final JobPostingRepository jobPostingRepository;
	private final UserTermsAgreementRepository userTermsAgreementRepository;

	public UserInfoResponse getUserInfo (UUID uuid) { // 사용자 정보 조회
		User user = userRepository.findByUuid(uuid)
			.orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
		return UserInfoResponse.from(user);
	}

	@Transactional
	public void updateProfile (UUID uuid, UpdateProfileRequest request) { // 사용자 정보 수정
		User user = userRepository.findByUuid(uuid)
			.orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

		handleEmailUpdate(user, request.getEmail()); // 이메일 수정

		String newNickname = request.getNickname(); // 닉네임 수정
		if (!user.getProfile().getNickname().equals(newNickname)) {
			validateDuplicateNickname(newNickname);
		}

		String newProfileImageURL = request.getProfileImageUrl(); // 프로필 이미지 수정
		String finalImageURL =
			(newProfileImageURL != null) ? newProfileImageURL : user.getProfile().getProfileImageUrl();

		user.getProfile().update(newNickname, finalImageURL);
	}

	public void validateDuplicateNickname (String nickname) { // 닉네임 중복 확인
		if (profileRepository.existsByNickname(nickname)) {
			throw new DuplicateNicknameException("이미 존재하는 닉네임입니다.");
		}
	}

	public void checkEmailDuplicate (String email) { // 이메일 중복 확인
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
		}
	}

	public void handleEmailUpdate (User user, String newEmail) { // 로컬 계정 확인 및 이메일 인증 여부 확인
		if (!user.getEmail().equals(newEmail)) {
			if (user.getProvider() != Provider.LOCAL) {
				throw new IllegalStateException("소셜 로그인 계정은 이메일 변경이 불가능합니다.");
			}
			if (!emailVerificationService.isVerified(newEmail)) {
				throw new MailVerificationException("이메일 인증이 완료되지 않았습니다.");
			}
			user.changeEmail(newEmail);
			emailVerificationService.deleteVerifiedStatus(newEmail);
		}
	}

	@Transactional
	public void deleteUser (UUID uuid) { // 회원 탈퇴
		User user = userRepository.findByUuid(uuid)
			.orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

		answerAnalysisRepository.deleteAllBySessionUser(user);
		interviewAnswerRepository.deleteAllBySessionUser(user);
		interviewReportRepository.deleteAllBySessionUser(user);
		interviewQuestionRepository.deleteAllBySessionUser(user);
		interviewSessionRepository.deleteAllByUser(user);
		rankingBackupRepository.deleteByUser(user);
		curriculumRepository.deleteAllByUser(user);
		interviewScheduleRepository.deleteAllByUser(user);
		jobPostingRepository.deleteAllByUser(user);
		userTermsAgreementRepository.deleteAllByUser(user);
		userRepository.delete(user); // Profile은 CascadeType.ALL로 자동 삭제
	}
}
