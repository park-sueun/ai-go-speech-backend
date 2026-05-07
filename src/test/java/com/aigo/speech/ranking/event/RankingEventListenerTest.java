package com.aigo.speech.ranking.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aigo.speech.ranking.service.RankingService;

@ExtendWith(MockitoExtension.class)
class RankingEventListenerTest {
	@Mock
	RankingService rankingService;

	@InjectMocks
	RankingEventListener rankingEventListener;

	@Test
	@DisplayName("이벤트 수신 시 rankingService.updateRanking을 호출한다")
	void handleInterviewCompleted_callsUpdateRanking () {
		UUID userUuid = UUID.randomUUID();
		InterviewCompletedEvent event = new InterviewCompletedEvent(userUuid, 64, "서비스 기획자");

		rankingEventListener.handleInterviewCompleted(event);

		verify(rankingService).updateRanking(userUuid, 64, "서비스 기획자");
	}

	@Test
	@DisplayName("rankingService 예외 발생 시 삼키고 정상 종료한다 (면접 결과에 영향 없음)")
	void handleInterviewCompleted_exceptionSuppressed () {
		UUID userUuid = UUID.randomUUID();
		InterviewCompletedEvent event = new InterviewCompletedEvent(userUuid, 64, "서비스 기획자");

		willThrow(new RuntimeException("Redis 연결 실패"))
			.given(rankingService).updateRanking(any(), anyInt(), any());

		org.junit.jupiter.api.Assertions.assertDoesNotThrow(
			() -> rankingEventListener.handleInterviewCompleted(event)
		);
	}
}
