package com.aigo.speech.ai.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aigo.speech.ai.client.AiProvider;
import com.aigo.speech.ai.config.AiProperties;
import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.exception.AiException;
import com.aigo.speech.ai.exception.AiRateLimitException;
import com.aigo.speech.ai.exception.AiTimeoutException;
import com.aigo.speech.ai.prompt.PromptRenderer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {

	private final List<AiProvider> clients;
	private final AiProperties props;
	private final PromptRenderer promptRenderer;

	public AiService(List<AiProvider> clients, AiProperties props, PromptRenderer promptRenderer) {
		this.clients = clients.stream()
			.sorted(Comparator.comparingInt(AiProvider::getPriority))
			.toList();
		this.props = props;
		this.promptRenderer = promptRenderer;
	}

	/**
	 * 동기 호출. ai.provider 설정 시 해당 공급자만 사용, 미설정 시 우선순위 순 폴백.
	 */
	public AiResponse complete(AiPromptRequest request) {
		String renderedPrompt = promptRenderer.render(request.template(), request.variables());
		String targetProvider = props.provider();

		List<AiProvider> targets = (targetProvider != null && !targetProvider.isBlank())
			? clients.stream().filter(c -> c.getProvider().equals(targetProvider)).toList()
			: clients;

		AiException lastException = null;

		for (AiProvider client : targets) {
			if (!client.isEnabled()) continue;
			try {
				AiResponse response = callWithRetry(client, request, renderedPrompt);
				log.info("[AI] 완료. provider={}, model={}", response.provider(), response.model());
				return response;
			} catch (AiException e) {
				log.warn("[AI] 공급자 실패, 폴백 시도. provider={}, reason={}", client.getProvider(), e.getMessage());
				lastException = e;
			}
		}

		throw new AiException("모든 AI 공급자 호출에 실패했습니다.", lastException);
	}

	/**
	 * 비동기 호출. aiExecutor 스레드풀에서 실행됩니다.
	 */
	@Async("aiExecutor")
	public CompletableFuture<AiResponse> completeAsync(AiPromptRequest request) {
		return CompletableFuture.completedFuture(complete(request));
	}

	private AiResponse callWithRetry(AiProvider client, AiPromptRequest request, String renderedPrompt) {
		int maxAttempts = props.retry().maxAttempts();
		long delayMs = props.retry().delayMs();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.debug("[AI] 호출 시작. provider={}, attempt={}/{}", client.getProvider(), attempt, maxAttempts);
				return client.complete(request, renderedPrompt);
			} catch (AiRateLimitException e) {
				if (attempt < maxAttempts) {
					long waitMs = delayMs * attempt;
					log.warn("[AI] Rate limit, {}ms 후 재시도. provider={}, attempt={}/{}", waitMs, client.getProvider(), attempt, maxAttempts);
					sleep(waitMs);
					continue;
				}
				throw e;
			} catch (AiTimeoutException e) {
				if (attempt < maxAttempts) {
					log.warn("[AI] Timeout, 즉시 재시도. provider={}, attempt={}/{}", client.getProvider(), attempt, maxAttempts);
					continue;
				}
				throw e;
			}
		}
		throw new AiException(client.getProvider() + " 재시도 횟수 초과");
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AiException("AI 요청이 중단되었습니다.");
		}
	}
}
