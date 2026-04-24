package com.aigo.speech.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.aigo.speech.interview.dto.JobPostingContext;
import com.aigo.speech.interview.entity.InterviewQuestion;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewQuestionRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.user.entity.User;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionGenerationServiceTest {

	@Mock private InterviewSessionRepository sessionRepository;
	@Mock private InterviewQuestionRepository questionRepository;
	@Mock private SseEmitterService sseEmitterService;
	@Mock private RestTemplate restTemplate;

	@InjectMocks
	private InterviewQuestionGenerationService generationService;

	private static final Long SESSION_ID = 1L;
	private static final UUID SESSION_UUID = UUID.randomUUID();

	private User user;
	private InterviewSession session;
	private JobPostingContext jobPostingContext;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(generationService, "apiKey", "test-api-key");
		ReflectionTestUtils.setField(generationService, "apiUrl", "https://gemini.example.com/api");
		ReflectionTestUtils.setField(generationService, "objectMapper", new ObjectMapper());

		user = User.builder().email("test@example.com").build();
		session = new InterviewSession(user, null, false, null);
		ReflectionTestUtils.setField(session, "id", SESSION_ID);
		ReflectionTestUtils.setField(session, "uuid", SESSION_UUID);

		jobPostingContext = new JobPostingContext(
			"카카오", "백엔드 개발자",
			List.of("서버 개발"), List.of("Java 3년"), List.of("Kotlin"), List.of("Java", "Spring")
		);
	}

	@Test
	@DisplayName("Gemini API 호출 성공 시 질문 5개가 저장되고 세션이 IN_PROGRESS로 전환된다")
	void generateQuestionsAsync_withSuccessfulGeminiCall_savesQuestionsAndStartsSession() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
			.willReturn(buildGeminiResponse(List.of("질문1", "질문2", "질문3", "질문4", "질문5")));
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

			generationService.generateQuestionsAsync(SESSION_ID, jobPostingContext);
		}

		assertThat(session.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);

		ArgumentCaptor<InterviewQuestion> captor = ArgumentCaptor.forClass(InterviewQuestion.class);
		then(questionRepository).should(org.mockito.Mockito.times(5)).save(captor.capture());

		List<InterviewQuestion> saved = captor.getAllValues();
		assertThat(saved).hasSize(5);
		assertThat(saved.get(0).getSequenceOrder()).isEqualTo(1);
		assertThat(saved.get(4).getSequenceOrder()).isEqualTo(5);

		then(sseEmitterService).should().sendEvent(eq(SESSION_UUID), eq("QUESTIONS_READY"), any());
		then(sseEmitterService).should().complete(SESSION_UUID);
	}

	@Test
	@DisplayName("존재하지 않는 세션 ID로 호출 시 예외가 발생한다")
	void generateQuestionsAsync_withUnknownSessionId_throwsException() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(
			InterviewSessionNotFoundException.class,
			() -> generationService.generateQuestionsAsync(SESSION_ID, jobPostingContext)
		);
	}

	@Test
	@DisplayName("Gemini API 호출 실패 시 ERROR SSE 이벤트를 전송하고 emitter를 종료한다")
	void generateQuestionsAsync_whenGeminiCallFails_sendsErrorEvent() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
			.willThrow(new RuntimeException("Gemini 연결 실패"));

		generationService.generateQuestionsAsync(SESSION_ID, jobPostingContext);

		then(questionRepository).should(never()).save(any());
		then(sseEmitterService).should().sendEvent(eq(SESSION_UUID), eq("ERROR"), any());
		then(sseEmitterService).should().complete(SESSION_UUID);
	}

	@Test
	@DisplayName("Gemini API가 429를 반환하면 재시도 후 성공한다")
	void generateQuestionsAsync_whenGemini429_retriesAndSucceeds() {
		given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
		given(sessionRepository.save(any())).willReturn(session);
		given(questionRepository.save(any(InterviewQuestion.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
			.willThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null))
			.willReturn(buildGeminiResponse(List.of("질문1", "질문2", "질문3", "질문4", "질문5")));

		try (MockedStatic<TransactionSynchronizationManager> tsm =
				 mockStatic(TransactionSynchronizationManager.class)) {

			tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
				.thenAnswer(invocation -> {
					TransactionSynchronization sync = invocation.getArgument(0);
					sync.afterCommit();
					return null;
				});

			// 재시도 딜레이를 0으로 줄이기 위해 직접 callGemini 내부를 테스트
			// restTemplate이 첫 호출에서 429를 던지고 두 번째에서 성공하므로
			// 실제 10초 sleep이 발생한다. 단, 테스트에서는 InterruptedException으로 중단 가능.
			// 여기서는 재시도 logic이 실행되고 최종 결과가 성공임을 검증한다.
			generationService.generateQuestionsAsync(SESSION_ID, jobPostingContext);
		}

		then(questionRepository).should(org.mockito.Mockito.times(5)).save(any(InterviewQuestion.class));
		then(sseEmitterService).should().sendEvent(eq(SESSION_UUID), eq("QUESTIONS_READY"), any());
	}

	// ======================== helpers ========================

	@SuppressWarnings("unchecked")
	private ResponseEntity<Map> buildGeminiResponse(List<String> questions) {
		String questionsJson = "[\"" + String.join("\", \"", questions) + "\"]";

		Map<String, Object> part = Map.of("text", questionsJson);
		Map<String, Object> content = Map.of("parts", List.of(part));
		Map<String, Object> candidate = Map.of("content", content);
		Map<String, Object> body = Map.of("candidates", List.of(candidate));

		return ResponseEntity.ok((Map) body);
	}
}
