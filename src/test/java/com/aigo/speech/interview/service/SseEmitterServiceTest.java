package com.aigo.speech.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterServiceTest {

	private SseEmitterService sseEmitterService;
	private static final UUID SESSION_UUID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		sseEmitterService = new SseEmitterService();
	}

	@Test
	@DisplayName("register 호출 시 SseEmitter가 반환된다")
	void register_returnsEmitter() {
		SseEmitter emitter = sseEmitterService.register(SESSION_UUID);

		assertThat(emitter).isNotNull();
	}

	@Test
	@DisplayName("동일 UUID로 register 재호출 시 새 emitter로 교체된다")
	void register_withSameUuid_replacesExistingEmitter() {
		SseEmitter first = sseEmitterService.register(SESSION_UUID);
		SseEmitter second = sseEmitterService.register(SESSION_UUID);

		assertThat(second).isNotSameAs(first);
	}

	@Test
	@DisplayName("emitter가 없는 UUID로 sendEvent 호출 시 예외 없이 처리된다")
	void sendEvent_withUnregisteredUuid_doesNotThrow() {
		assertThatNoException().isThrownBy(
			() -> sseEmitterService.sendEvent(UUID.randomUUID(), "TEST_EVENT", "data")
		);
	}

	@Test
	@DisplayName("emitter가 없는 UUID로 complete 호출 시 예외 없이 처리된다")
	void complete_withUnregisteredUuid_doesNotThrow() {
		assertThatNoException().isThrownBy(
			() -> sseEmitterService.complete(UUID.randomUUID())
		);
	}

	@Test
	@DisplayName("complete 호출 후 동일 UUID로 sendEvent 호출 시 예외 없이 처리된다")
	void complete_thenSendEvent_doesNotThrow() {
		sseEmitterService.register(SESSION_UUID);
		sseEmitterService.complete(SESSION_UUID);

		assertThatNoException().isThrownBy(
			() -> sseEmitterService.sendEvent(SESSION_UUID, "TEST_EVENT", "data")
		);
	}
}
