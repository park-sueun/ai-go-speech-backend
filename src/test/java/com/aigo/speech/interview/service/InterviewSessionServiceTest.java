package com.aigo.speech.interview.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.interview.dto.CreateSessionRequest;
import com.aigo.speech.interview.dto.InterviewSessionResponse;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.repository.InterviewQuestionRepository;
import com.aigo.speech.question.service.InterviewQuestionService;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InterviewSessionServiceTest {

	@Mock private InterviewSessionRepository sessionRepository;
	@Mock private InterviewQuestionRepository questionRepository;
	@Mock private UserRepository userRepository;
	@Mock private JobPostingRepository jobPostingRepository;
	@Mock private SseEmitterService sseEmitterService;
	@Mock private InterviewQuestionService questionGenerationService;

	@InjectMocks
	private InterviewSessionService sessionService;

	private static final UUID USER_UUID = UUID.randomUUID();
	private static final UUID JOB_POSTING_UUID = UUID.randomUUID();
	private static final UUID SESSION_UUID = UUID.randomUUID();

	private User user;
	private JobPosting jobPosting;

	@BeforeEach
	void setUp() {
		user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		jobPosting = JobPosting.pending(user, "https://wanted.co.kr/wd/1");
		ReflectionTestUtils.setField(jobPosting, "uuid", JOB_POSTING_UUID);
	}

	// ======================== createSession ========================

	@Test
	@DisplayName("존재하지 않는 사용자로 세션 생성 시 예외가 발생한다")
	void createSession_withUnknownUser_throwsException() {
		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> sessionService.createSession(USER_UUID.toString(), buildRequest(JOB_POSTING_UUID)))
			.isInstanceOf(UserNotFoundException.class);

		then(sessionRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("유효한 jobPostingUuid로 세션 생성 시 JobPosting이 연결되고 질문 생성이 트리거된다")
	void createSession_withValidJobPosting_savesSessionAndTriggersQuestionGeneration() {
		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(jobPostingRepository.findByUuid(JOB_POSTING_UUID)).willReturn(Optional.of(jobPosting));

		InterviewSession saved = new InterviewSession(user, jobPosting, false, LocalDate.of(2026, 5, 10));
		ReflectionTestUtils.setField(saved, "id", 1L);
		given(sessionRepository.save(any(InterviewSession.class))).willReturn(saved);

		InterviewSessionResponse response = sessionService.createSession(USER_UUID.toString(), buildRequest(JOB_POSTING_UUID));

		assertThat(response.status()).isEqualTo("READY");
		then(questionGenerationService).should().generateQuestionsAsync(1L);
	}

	@Test
	@DisplayName("존재하지 않는 jobPostingUuid로 세션 생성 시 예외가 발생한다")
	void createSession_withUnknownJobPosting_throwsException() {
		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(jobPostingRepository.findByUuid(JOB_POSTING_UUID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> sessionService.createSession(USER_UUID.toString(), buildRequest(JOB_POSTING_UUID)))
			.isInstanceOf(InterviewSessionNotFoundException.class);

		then(sessionRepository).should(never()).save(any());
	}

	// ======================== getSession ========================

	@Test
	@DisplayName("존재하지 않는 UUID로 세션 조회 시 예외가 발생한다")
	void getSession_withUnknownUuid_throwsException() {
		given(sessionRepository.findByUuid(SESSION_UUID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> sessionService.getSession(USER_UUID.toString(), SESSION_UUID))
			.isInstanceOf(InterviewSessionNotFoundException.class);
	}

	@Test
	@DisplayName("질문이 없는 READY 세션 조회 시 questions가 null이다")
	void getSession_whenNoQuestionsExist_returnsNullQuestions() {
		InterviewSession session = new InterviewSession(user, null, false, null);
		given(sessionRepository.findByUuid(SESSION_UUID)).willReturn(Optional.of(session));
		given(questionRepository.findBySessionOrderBySequenceOrderAsc(session)).willReturn(List.of());

		InterviewSessionResponse response = sessionService.getSession(USER_UUID.toString(), SESSION_UUID);

		assertThat(response.status()).isEqualTo("READY");
		assertThat(response.questions()).isNull();
	}

	@Test
	@DisplayName("질문이 있는 IN_PROGRESS 세션 조회 시 질문 목록이 반환된다")
	void getSession_whenQuestionsExist_returnsQuestions() {
		InterviewSession session = new InterviewSession(user, null, false, null);
		session.start();

		InterviewQuestion q1 = new InterviewQuestion(session, "자기소개를 해주세요.", 1);
		InterviewQuestion q2 = new InterviewQuestion(session, "지원 동기를 말씀해주세요.", 2);

		given(sessionRepository.findByUuid(SESSION_UUID)).willReturn(Optional.of(session));
		given(questionRepository.findBySessionOrderBySequenceOrderAsc(session)).willReturn(List.of(q1, q2));

		InterviewSessionResponse response = sessionService.getSession(USER_UUID.toString(), SESSION_UUID);

		assertThat(response.status()).isEqualTo("IN_PROGRESS");
		assertThat(response.questions()).hasSize(2);
		assertThat(response.questions().get(0).sequenceOrder()).isEqualTo(1);
		assertThat(response.questions().get(0).content()).isEqualTo("자기소개를 해주세요.");
	}

	// ======================== subscribeToSession ========================

	@Test
	@DisplayName("존재하는 세션 UUID로 SSE 구독 시 emitter가 등록된다")
	void subscribeToSession_withValidUuid_registersEmitter() {
		InterviewSession session = new InterviewSession(user, null, false, null);
		given(sessionRepository.findByUuid(SESSION_UUID)).willReturn(Optional.of(session));

		sessionService.subscribeToSession(USER_UUID.toString(), SESSION_UUID);

		then(sseEmitterService).should().register(SESSION_UUID);
	}

	@Test
	@DisplayName("존재하지 않는 세션 UUID로 SSE 구독 시 예외가 발생한다")
	void subscribeToSession_withUnknownUuid_throwsException() {
		given(sessionRepository.findByUuid(SESSION_UUID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> sessionService.subscribeToSession(USER_UUID.toString(), SESSION_UUID))
			.isInstanceOf(InterviewSessionNotFoundException.class);
	}

	// ======================== helpers ========================

	private CreateSessionRequest buildRequest(UUID jobPostingUuid) {
		CreateSessionRequest request = new CreateSessionRequest();
		ReflectionTestUtils.setField(request, "jobPostingUuid", jobPostingUuid);
		ReflectionTestUtils.setField(request, "retry", false);
		ReflectionTestUtils.setField(request, "interviewDate", LocalDate.of(2026, 5, 10));
		return request;
	}
}
