package com.aigo.speech.ai.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.aigo.speech.ai.config.AiProperties;
import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.exception.AiException;
import com.aigo.speech.ai.exception.AiRateLimitException;
import com.aigo.speech.ai.exception.AiServerOverloadException;
import com.aigo.speech.ai.exception.AiTimeoutException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ClaudeProvider implements AiProvider {

	private static final String PROVIDER = "claude";
	private static final String ANTHROPIC_VERSION = "2023-06-01";

	private final WebClient webClient;
	private final AiProperties props;

	public ClaudeProvider(@Qualifier("aiWebClient") WebClient webClient, AiProperties props) {
		this.webClient = webClient;
		this.props = props;
	}

	@Override
	public String getProvider() {
		return PROVIDER;
	}

	@Override
	public int getPriority() {
		return props.claude().priority();
	}

	@Override
	public boolean isEnabled() {
		return props.claude().enabled();
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		Map<String, Object> body = Map.of(
			"model", props.claude().model(),
			"max_tokens", 4096,
			"system", systemPrompt,
			"messages", List.of(Map.of("role", "user", "content", userPrompt))
		);
		return callApi(body);
	}

	@Override
	public AiResponse complete(AiPromptRequest request, String renderedPrompt) {
		Map<String, Object> body = Map.of(
			"model", props.claude().model(),
			"max_tokens", request.maxTokens(),
			"messages", List.of(Map.of("role", "user", "content", renderedPrompt))
		);
		String content = callApi(body);
		log.debug("[Claude] 응답 수신. contentLength={}", content.length());
		return new AiResponse(PROVIDER, props.claude().model(), content);
	}

	@SuppressWarnings("unchecked")
	private String callApi(Map<String, Object> body) {
		try {
			Map<?, ?> responseBody = webClient.post()
				.uri(props.claude().apiUrl())
				.headers(headers -> {
					headers.setContentType(MediaType.APPLICATION_JSON);
					headers.set("x-api-key", props.claude().apiKey());
					headers.set("anthropic-version", ANTHROPIC_VERSION);
				})
				.bodyValue(body)
				.retrieve()
				.onStatus(
					status -> status.value() == 503,
					response -> Mono.error(new AiServerOverloadException(PROVIDER))
				)
				.onStatus(
					status -> status.value() == 429,
					response -> Mono.error(new AiRateLimitException(PROVIDER))
				)
				.onStatus(
					HttpStatusCode::isError,
					response -> response.bodyToMono(String.class)
						.flatMap(b -> {
							log.error("[Claude] API 오류 응답: status={}, body={}", response.statusCode().value(), b);
							return Mono.error(new AiException("Claude API 호출 실패: " + response.statusCode().value()));
						})
				)
				.bodyToMono(Map.class)
				.block();

			List<Map<?, ?>> contentList = (List<Map<?, ?>>) responseBody.get("content");
			return (String) contentList.get(0).get("text");

		} catch (AiException e) {
			throw e;
		} catch (WebClientRequestException e) {
			if (isTimeout(e)) throw new AiTimeoutException(PROVIDER);
			log.error("[Claude] 연결 실패: {}", e.getMessage());
			throw new AiException("Claude API 연결 실패", e);
		} catch (Exception e) {
			throw new AiException("Claude 응답 파싱 실패", e);
		}
	}

	private boolean isTimeout(Throwable e) {
		Throwable cause = e;
		while (cause != null) {
			if (cause instanceof java.util.concurrent.TimeoutException) return true;
			if (cause.getClass().getSimpleName().contains("TimeoutException")) return true;
			cause = cause.getCause();
		}
		return false;
	}
}
