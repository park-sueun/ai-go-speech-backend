package com.aigo.speech.global.sse;

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

	public SseEmitter register(UUID uuid) {
		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

		emitter.onTimeout(() -> {
			log.warn("[SSE] Timeout. uuid={}", uuid);
			emitters.remove(uuid);
		});
		emitter.onError(e -> {
			log.warn("[SSE] Error. uuid={}, message={}", uuid, e.getMessage());
			emitters.remove(uuid);
		});
		emitter.onCompletion(() -> emitters.remove(uuid));

		emitters.put(uuid, emitter);
		return emitter;
	}

	public void sendEvent(UUID uuid, String eventType, Object data) {
		SseEmitter emitter = emitters.get(uuid);
		if (emitter == null) {
			log.warn("[SSE] Emitter not found. uuid={}", uuid);
			return;
		}
		try {
			emitter.send(
				SseEmitter.event()
					.name(eventType)
					.data(data, MediaType.APPLICATION_JSON)
			);
		} catch (IOException e) {
			log.warn("[SSE] Send failed. uuid={}, message={}", uuid, e.getMessage());
			emitters.remove(uuid);
		}
	}

	public void complete(UUID uuid) {
		SseEmitter emitter = emitters.get(uuid);
		if (emitter != null) {
			emitter.complete();
			emitters.remove(uuid);
		}
	}
}
