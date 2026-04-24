package com.aigo.speech.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

import com.aigo.speech.interview.dto.SubmitAnswerRequest;
import com.aigo.speech.interview.dto.SubmitAnswerResponse;
import com.aigo.speech.interview.entity.AnswerScore;
import com.aigo.speech.interview.entity.InterviewAnswer;
import com.aigo.speech.interview.entity.InterviewQuestion;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.interview.exception.QuestionNotFoundException;
import com.aigo.speech.interview.repository.AnswerScoreRepository;
import com.aigo.speech.interview.repository.InterviewAnswerRepository;
import com.aigo.speech.interview.repository.InterviewQuestionRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.user.entity.User;

@ExtendWith(MockitoExtension.class)
class InterviewAnswerServiceTest {

	@Mock private InterviewSessionRepository sessionRepository;
	@Mock private InterviewQuestionRepository questionRepository;
	@Mock private InterviewAnswerRepository answerRepository;
	@Mock private AnswerScoreRepository answerScoreRepository;
	@Mock private SseEmitterService sseEmitterService;
	@Mock private InterviewSessionService sessionService;

	@InjectMocks
	private InterviewAnswerService answerService;

	private static final UUID SESSION_UUID = UUID.randomUUID();
	private static final UUID QUESTION_UUID = UUID.randomUUID();

	private User user;
	private InterviewSession session;
	private InterviewQuestion question;

	@BeforeEach
	void setUp() {
		user = User.builder().email("test@example.com").build();
		session = new InterviewSession(user, null, false, null);
		session.start();
		ReflectionTestUtils.setField(session, "id", 1L);
		ReflectionTestUtils.setField(session, "uuid", SESSION_UUID);

		question = new InterviewQuestion(session, "자기소개를 해주세요.", 1);
		ReflectionTestUtils.setField(question, "uuid", QUESTION_UUID);
	}

	// ======================== 정상 케이스 ========================

	@Test
	@DisplayName("정상 답변 제출 시 답변과 점수 데이터가 저장된다")
	void submitAnswer_savesAnswerAndScore() {
		InterviewAnswer savedAnswer = new InterviewAnswer(
			question, "https://storage.com/audio.wav", "안녕하세요", 90,
			null, null, null
		);
		given(sessionService.findByUuid(SESSION_UUID)).willReturn(session);
		given(questionRepository.findByUuid(QUESTION_UUID)).willReturn(Optional.of(question));
		given(answerRepository.save(any(InterviewAnswer.class))).willReturn(savedAnswer);
		given(answerScoreRepository.save(any(AnswerScore.class))).willReturn(new AnswerScore(savedAnswer, 2, 3000, 90000));
		given(questionRepository.countBySession(session)).willReturn(5L);
		given(answerRepository.countBySession(session)).willReturn(1L);

		SubmitAnswerResponse response = answerService.submitAnswer(SESSION_UUID, QUESTION_UUID, buildRequest());

		assertThat(response.sessionCompleted()).isFalse();
		then(answerRepository).should().save(any(InterviewAnswer.class));
		then(answerScoreRepository).should().save(any(AnswerScore.class));
	}

	@Test
	@DisplayName("마지막 답변 제출 시 세션이 COMPLETED로 전환되고 SSE 이벤트가 전송된다")
	void submitAnswer_whenLastAnswer_completesSessionAndSendsSSE() {
		InterviewAnswer savedAnswer = new InterviewAnswer(
			question, "https://storage.com/audio.wav", null, null,
			null, null, null
		);
		given(sessionService.findByUuid(SESSION_UUID)).willReturn(session);
		given(questionRepository.findByUuid(QUESTION_UUID)).willReturn(Optional.of(question));
		given(answerRepository.save(any(InterviewAnswer.class))).willReturn(savedAnswer);
		given(answerScoreRepository.save(any(AnswerScore.class))).willReturn(new AnswerScore(savedAnswer, null, null, null));
		given(questionRepository.countBySession(session)).willReturn(5L);
		given(answerRepository.countBySession(session)).willReturn(5L);
		given(sessionRepository.save(any(InterviewSession.class))).willReturn(session);

		SubmitAnswerResponse response = answerService.submitAnswer(SESSION_UUID, QUESTION_UUID, buildRequest());

		assertThat(response.sessionCompleted()).isTrue();
		assertThat(session.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
		then(sseEmitterService).should().sendEvent(any(), any(), any());
		then(sseEmitterService).should().complete(SESSION_UUID);
	}

	// ======================== 예외 케이스 ========================

	@Test
	@DisplayName("READY 상태 세션에 답변 제출 시 예외가 발생한다")
	void submitAnswer_whenSessionReady_throwsException() {
		InterviewSession readySession = new InterviewSession(user, null, false, null);
		ReflectionTestUtils.setField(readySession, "uuid", SESSION_UUID);
		given(sessionService.findByUuid(SESSION_UUID)).willReturn(readySession);

		assertThatThrownBy(() -> answerService.submitAnswer(SESSION_UUID, QUESTION_UUID, buildRequest()))
			.isInstanceOf(InvalidSessionStatusException.class)
			.hasMessage("진행 중인 면접 세션이 아닙니다.");

		then(answerRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("COMPLETED 상태 세션에 답변 제출 시 예외가 발생한다")
	void submitAnswer_whenSessionCompleted_throwsException() {
		session.complete();
		given(sessionService.findByUuid(SESSION_UUID)).willReturn(session);

		assertThatThrownBy(() -> answerService.submitAnswer(SESSION_UUID, QUESTION_UUID, buildRequest()))
			.isInstanceOf(InvalidSessionStatusException.class);

		then(answerRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("존재하지 않는 질문 UUID로 답변 제출 시 예외가 발생한다")
	void submitAnswer_withUnknownQuestion_throwsException() {
		given(sessionService.findByUuid(SESSION_UUID)).willReturn(session);
		given(questionRepository.findByUuid(QUESTION_UUID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> answerService.submitAnswer(SESSION_UUID, QUESTION_UUID, buildRequest()))
			.isInstanceOf(QuestionNotFoundException.class)
			.hasMessage("질문을 찾을 수 없습니다.");

		then(answerRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("다른 세션의 질문으로 답변 제출 시 예외가 발생한다")
	void submitAnswer_whenQuestionBelongsToDifferentSession_throwsException() {
		InterviewSession otherSession = new InterviewSession(user, null, false, null);
		otherSession.start();
		ReflectionTestUtils.setField(otherSession, "id", 99L);

		InterviewQuestion otherQuestion = new InterviewQuestion(otherSession, "다른 세션의 질문", 1);
		ReflectionTestUtils.setField(otherQuestion, "uuid", QUESTION_UUID);

		given(sessionService.findByUuid(SESSION_UUID)).willReturn(session);
		given(questionRepository.findByUuid(QUESTION_UUID)).willReturn(Optional.of(otherQuestion));

		assertThatThrownBy(() -> answerService.submitAnswer(SESSION_UUID, QUESTION_UUID, buildRequest()))
			.isInstanceOf(QuestionNotFoundException.class)
			.hasMessage("해당 세션의 질문이 아닙니다.");
	}

	// ======================== helpers ========================

	private SubmitAnswerRequest buildRequest() {
		SubmitAnswerRequest request = new SubmitAnswerRequest();
		ReflectionTestUtils.setField(request, "audioUrl", "https://storage.com/audio.wav");
		ReflectionTestUtils.setField(request, "sttText", "안녕하세요");
		ReflectionTestUtils.setField(request, "duration", 90);
		ReflectionTestUtils.setField(request, "silenceCount", 2);
		ReflectionTestUtils.setField(request, "totalSilenceDuration", 3000);
		ReflectionTestUtils.setField(request, "answerDuration", 90000);
		return request;
	}
}
