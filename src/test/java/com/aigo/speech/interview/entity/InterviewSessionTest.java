package com.aigo.speech.interview.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigo.speech.user.entity.User;

class InterviewSessionTest {

	private User user;
	private InterviewSession session;

	@BeforeEach
	void setUp() {
		user = User.builder().email("test@example.com").build();
		session = new InterviewSession(user, null, false, LocalDate.of(2026, 5, 10));
	}

	@Test
	@DisplayName("세션 생성 시 READY 상태이고 UUID와 createdAt이 설정된다")
	void constructor_setsReadyStatusAndMetadata() {
		assertThat(session.getStatus()).isEqualTo(InterviewStatus.READY);
		assertThat(session.getUuid()).isNotNull();
		assertThat(session.getCreatedAt()).isNotNull();
		assertThat(session.getInterviewDate()).isEqualTo(LocalDate.of(2026, 5, 10));
		assertThat(session.getStartedAt()).isNull();
		assertThat(session.getEndedAt()).isNull();
	}

	@Test
	@DisplayName("READY 상태에서 start() 호출 시 IN_PROGRESS로 전환되고 startedAt이 설정된다")
	void start_whenReady_transitionsToInProgress() {
		session.start();

		assertThat(session.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
		assertThat(session.getStartedAt()).isNotNull();
		assertThat(session.getStartedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
	}

	@Test
	@DisplayName("READY가 아닌 상태에서 start() 호출 시 상태가 변경되지 않는다")
	void start_whenNotReady_doesNothing() {
		session.start();
		LocalDateTime firstStartedAt = session.getStartedAt();

		session.start();

		assertThat(session.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
		assertThat(session.getStartedAt()).isEqualTo(firstStartedAt);
	}

	@Test
	@DisplayName("IN_PROGRESS 상태에서 complete() 호출 시 COMPLETED로 전환되고 endedAt이 설정된다")
	void complete_whenInProgress_transitionsToCompleted() {
		session.start();

		session.complete();

		assertThat(session.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
		assertThat(session.getEndedAt()).isNotNull();
		assertThat(session.getEndedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
	}

	@Test
	@DisplayName("IN_PROGRESS가 아닌 상태에서 complete() 호출 시 상태가 변경되지 않는다")
	void complete_whenNotInProgress_doesNothing() {
		session.complete();

		assertThat(session.getStatus()).isEqualTo(InterviewStatus.READY);
		assertThat(session.getEndedAt()).isNull();
	}

	@Test
	@DisplayName("retry=true로 생성된 세션은 retry 값이 true다")
	void constructor_withRetry_setsRetryTrue() {
		InterviewSession retrySession = new InterviewSession(user, null, true, null);

		assertThat(retrySession.isRetry()).isTrue();
	}

	@Test
	@DisplayName("interviewDate가 null인 경우에도 세션이 정상 생성된다")
	void constructor_withNullInterviewDate_createsSession() {
		InterviewSession sessionWithoutDate = new InterviewSession(user, null, false, null);

		assertThat(sessionWithoutDate.getInterviewDate()).isNull();
		assertThat(sessionWithoutDate.getStatus()).isEqualTo(InterviewStatus.READY);
	}
}
