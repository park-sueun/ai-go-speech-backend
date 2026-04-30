package com.aigo.speech.jobposting.service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JobPostingSseEmitterService {

	private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;

	private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

	public SseEmitter register(UUID jobPostingUuid) {
		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

		emitter.onTimeout(() -> {
			log.warn("[SSE][JobPosting] Timeout. uuid={}", jobPostingUuid);
			emitters.remove(jobPostingUuid);
		});
		emitter.onError(e -> {
			log.warn("[SSE][JobPosting] Error. uuid={}, message={}", jobPostingUuid, e.getMessage());
			emitters.remove(jobPostingUuid);
		});
		emitter.onCompletion(() -> emitters.remove(jobPostingUuid));

		emitters.put(jobPostingUuid, emitter);
		return emitter;
	}

	public void sendEvent(UUID jobPostingUuid, String eventType, Object data) {
		SseEmitter emitter = emitters.get(jobPostingUuid);
		if (emitter == null) {
			log.warn("[SSE][JobPosting] Emitter not found. uuid={}", jobPostingUuid);
			return;
		}
		try {
			emitter.send(
				SseEmitter.event()
					.name(eventType)
					.data(data, MediaType.APPLICATION_JSON)
			);
		} catch (IOException e) {
			log.warn("[SSE][JobPosting] Send failed. uuid={}, message={}", jobPostingUuid, e.getMessage());
			emitters.remove(jobPostingUuid);
		}
	}

	public void complete(UUID jobPostingUuid) {
		SseEmitter emitter = emitters.get(jobPostingUuid);
		if (emitter != null) {
			emitter.complete();
			emitters.remove(jobPostingUuid);
		}
	}
}
