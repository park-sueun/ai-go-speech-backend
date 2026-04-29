package com.aigo.speech.interview.service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SseEmitterService {

	private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;

	private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

	public SseEmitter register(UUID sessionUuid) {
		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

		emitter.onTimeout(() -> {
			log.warn("[SSE] Timeout. sessionUuid={}", sessionUuid);
			emitters.remove(sessionUuid);
		});
		emitter.onError(e -> {
			log.warn("[SSE] Error. sessionUuid={}, message={}", sessionUuid, e.getMessage());
			emitters.remove(sessionUuid);
		});
		emitter.onCompletion(() -> emitters.remove(sessionUuid));

		emitters.put(sessionUuid, emitter);
		return emitter;
	}

	public void sendEvent(UUID sessionUuid, String eventType, Object data) {
		SseEmitter emitter = emitters.get(sessionUuid);
		if (emitter == null) {
			log.warn("[SSE] Emitter not found. sessionUuid={}", sessionUuid);
			return;
		}
		try {
			emitter.send(
				SseEmitter.event()
					.name(eventType)
					.data(data, MediaType.APPLICATION_JSON)
			);
		} catch (IOException e) {
			log.warn("[SSE] Send failed. sessionUuid={}, message={}", sessionUuid, e.getMessage());
			emitters.remove(sessionUuid);
		}
	}

	public void complete(UUID sessionUuid) {
		SseEmitter emitter = emitters.get(sessionUuid);
		if (emitter != null) {
			emitter.complete();
			emitters.remove(sessionUuid);
		}
	}
}
