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
public class GeminiProvider implements AiProvider {

	private static final String PROVIDER = "gemini";

	private final WebClient webClient;
	private final AiProperties props;

	public GeminiProvider(@Qualifier("aiWebClient") WebClient webClient, AiProperties props) {
		this.webClient = webClient;
		this.props = props;
	}

	@Override
	public String getProvider() {
		return PROVIDER;
	}

	@Override
	public int getPriority() {
		return props.gemini().priority();
	}

	@Override
	public boolean isEnabled() {
		return props.gemini().enabled();
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		String fullPrompt = systemPrompt.isBlank() ? userPrompt : systemPrompt + "\n\n" + userPrompt;
		return callApi(fullPrompt, 4096);
	}

	@Override
	public AiResponse complete(AiPromptRequest request, String renderedPrompt) {
		String content = callApi(renderedPrompt, request.maxTokens());
		log.debug("[Gemini] 응답 수신. contentLength={}", content.length());
		return new AiResponse(PROVIDER, props.gemini().model(), content);
	}

	@SuppressWarnings("unchecked")
	private String callApi(String prompt, int maxTokens) {
		Map<String, Object> body = Map.of(
			"contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
			"generationConfig", Map.of(
				"temperature", 0.1,
				"maxOutputTokens", maxTokens
			)
		);

		try {
			Map<?, ?> responseBody = webClient.post()
				.uri(props.gemini().apiUrl())
				.headers(headers -> {
					headers.setContentType(MediaType.APPLICATION_JSON);
					headers.set("x-goog-api-key", props.gemini().apiKey());
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
							log.error("[Gemini] API 오류 응답: status={}, body={}", response.statusCode().value(), b);
							return Mono.error(new AiException("Gemini API 호출 실패: " + response.statusCode().value()));
						})
				)
				.bodyToMono(Map.class)
				.block();

			List<Map<?, ?>> candidates = (List<Map<?, ?>>) responseBody.get("candidates");
			Map<?, ?> content = (Map<?, ?>) candidates.get(0).get("content");
			List<Map<?, ?>> parts = (List<Map<?, ?>>) content.get("parts");
			// thinking 모델(gemini-2.5-*)은 parts[0]이 thought 파트 → thought가 아닌 첫 텍스트 파트를 사용
			String raw = parts.stream()
				.filter(part -> !Boolean.TRUE.equals(part.get("thought")))
				.map(part -> (String) part.get("text"))
				.filter(text -> text != null && !text.isBlank())
				.findFirst()
				.orElse("");
			return raw.replaceAll("(?s)```json\\s*|```\\s*", "").trim();

		} catch (AiException e) {
			throw e;
		} catch (WebClientRequestException e) {
			if (isTimeout(e)) throw new AiTimeoutException(PROVIDER);
			log.error("[Gemini] 연결 실패: {}", e.getMessage());
			throw new AiException("Gemini API 연결 실패", e);
		} catch (Exception e) {
			throw new AiException("Gemini 응답 파싱 실패", e);
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
