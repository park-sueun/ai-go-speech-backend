package com.aigo.speech.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.service.AiService;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.interview.service.SseEmitterService;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.entity.JobPostingStatus;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.repository.InterviewQuestionRepository;
import com.aigo.speech.user.entity.User;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionServiceTest {

	@Mock private InterviewSessionRepository sessionRepository;
	@Mock private InterviewQuestionRepository questionRepository;
	@Mock private SseEmitterService sseEmitterService;
	@Mock private AiService aiService;

	private InterviewQuestionService questionService;

	private static final Long SESSION_ID = 1L;
	private static final UUID SESSION_UUID = UUID.randomUUID();

	private User user;
	private JobPosting jobPosting;
	private InterviewSession session;

	@BeforeEach
	void setUp() {
		questionService = new InterviewQuestionService(
			sessionRepository, questionRepository, sseEmitterService, aiService, new ObjectMapper()
		);

		user = User.builder().email("test@example.com").build();

		jobPosting = JobPosting.pending(user, "https://wanted.co.kr/wd/1");
		ReflectionTestUtils.setField(jobPosting, "companyName", "카카오");
		ReflectionTestUtils.setField(jobPosting, "position", "백엔드 개발자");
		ReflectionTestUtils.setField(jobPosting, "responsibilities", List.of("서버 개발", "API 설계"));
		ReflectionTestUtils.setField(jobPosting, "requiredSkills", List.of("Java", "Spring Boot"));
		ReflectionTestUtils.setField(jobPosting, "preferredSkills", List.of("Kotlin"));
		ReflectionTestUtils.setField(jobPosting, "requiredExperience", "3년 이상");

		session = new InterviewSession(user, jobPosting, false, null);
		ReflectionTestUtils.setField(session, "id", SESSION_ID);
		ReflectionTestUtils.setField(session, "uuid", SESSION_UUID);
	}

	// ======================== 정상 케이스 ========================

	@Test
	@DisplayName("AI 호출 성공 시 질문 5개가 저장되고 세션이 IN_PROGRESS로 전환된다")
	void generateQuestionsAsync_success_savesQuestionsAndStartsSession() {
		String questionsJson = "[\"질문1\", \"질문2\", \"질문3\", \"질문4\", \"질문5\"]";
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(aiService.complete(any(AiPromptRequest.class)))
			.willReturn(new AiResponse("gemini", "gemini-2.0-flash", questionsJson));
		given(questionRepository.save(any(InterviewQuestion.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(sessionRepository.save(any(InterviewSession.class))).willReturn(session);

		try (MockedStatic<TransactionSynchronizationManager> tsm =
				 mockStatic(TransactionSynchronizationManager.class)) {

			tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
				.thenAnswer(invocation -> {
					TransactionSynchronization sync = invocation.getArgument(0);
					sync.afterCommit();
					return null;
				});

			questionService.generateQuestionsAsync(SESSION_ID);
		}

		assertThat(session.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);

		ArgumentCaptor<InterviewQuestion> captor = ArgumentCaptor.forClass(InterviewQuestion.class);
		then(questionRepository).should(times(5)).save(captor.capture());

		List<InterviewQuestion> saved = captor.getAllValues();
		assertThat(saved).hasSize(5);
		assertThat(saved.get(0).getSequenceOrder()).isEqualTo(1);
		assertThat(saved.get(0).getContent()).isEqualTo("질문1");
		assertThat(saved.get(4).getSequenceOrder()).isEqualTo(5);

		then(sseEmitterService).should().sendEvent(eq(SESSION_UUID), eq("QUESTIONS_READY"), any());
		then(sseEmitterService).should().complete(SESSION_UUID);
	}

	@Test
	@DisplayName("AI 호출 시 JobPosting 데이터가 변수 Map에 올바르게 담긴다")
	void generateQuestionsAsync_includesJobPostingDataInVariables() {
		String questionsJson = "[\"질문1\", \"질문2\", \"질문3\", \"질문4\", \"질문5\"]";
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(aiService.complete(any(AiPromptRequest.class)))
			.willReturn(new AiResponse("gemini", "gemini-2.0-flash", questionsJson));
		given(questionRepository.save(any())).willAnswer(i -> i.getArgument(0));
		given(sessionRepository.save(any())).willReturn(session);

		ArgumentCaptor<AiPromptRequest> promptCaptor = ArgumentCaptor.forClass(AiPromptRequest.class);

		try (MockedStatic<TransactionSynchronizationManager> tsm =
				 mockStatic(TransactionSynchronizationManager.class)) {
			tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
				.thenAnswer(i -> { i.getArgument(0, TransactionSynchronization.class).afterCommit(); return null; });

			questionService.generateQuestionsAsync(SESSION_ID);
		}

		then(aiService).should().complete(promptCaptor.capture());
		Map<String, String> variables = promptCaptor.getValue().variables();
		assertThat(variables.get("companyName")).isEqualTo("카카오");
		assertThat(variables.get("position")).isEqualTo("백엔드 개발자");
		assertThat(variables.get("requiredSkills")).isEqualTo("Java, Spring Boot");
		assertThat(variables.get("requiredExperience")).isEqualTo("3년 이상");
	}

	// ======================== 예외 케이스 ========================

	@Test
	@DisplayName("존재하지 않는 세션 ID로 호출 시 예외가 발생한다")
	void generateQuestionsAsync_withUnknownSessionId_throwsException() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> questionService.generateQuestionsAsync(SESSION_ID))
			.isInstanceOf(InterviewSessionNotFoundException.class);
	}

	@Test
	@DisplayName("AI 호출 실패 시 ERROR SSE 이벤트를 전송하고 emitter를 종료한다")
	void generateQuestionsAsync_whenAiFails_sendsErrorEvent() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(aiService.complete(any(AiPromptRequest.class)))
			.willThrow(new RuntimeException("AI 연결 실패"));

		questionService.generateQuestionsAsync(SESSION_ID);

		then(questionRepository).should(never()).save(any());
		then(sseEmitterService).should().sendEvent(eq(SESSION_UUID), eq("ERROR"), any());
		then(sseEmitterService).should().complete(SESSION_UUID);
	}

	@Test
	@DisplayName("AI 응답 파싱 실패 시 ERROR SSE 이벤트를 전송한다")
	void generateQuestionsAsync_whenParsingFails_sendsErrorEvent() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(aiService.complete(any(AiPromptRequest.class)))
			.willReturn(new AiResponse("gemini", "gemini-2.0-flash", "이것은 JSON이 아닙니다"));

		questionService.generateQuestionsAsync(SESSION_ID);

		then(questionRepository).should(never()).save(any());
		then(sseEmitterService).should().sendEvent(eq(SESSION_UUID), eq("ERROR"), any());
		then(sseEmitterService).should().complete(SESSION_UUID);
	}
}
