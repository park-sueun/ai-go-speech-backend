package com.aigo.speech.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.aigo.speech.ai.client.GeminiProvider;
import com.aigo.speech.ai.config.AiProperties;
import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.prompt.PromptRenderer;
import com.aigo.speech.ai.prompt.PromptTemplate;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class AiServiceIntegrationTest {

	private static final String KAKAO_JOB_POSTING = """
		카카오 서버 개발자 채용

		[카카오 공동체 소개]
		카카오는 모바일 메신저 카카오톡을 비롯하여 다양한 플랫폼 서비스를 운영하는 IT 기업입니다.

		[담당 업무]
		- 카카오 서비스를 위한 백엔드 서버 개발 및 운영
		- 대용량 트래픽 처리를 위한 분산 시스템 설계
		- 마이크로서비스 아키텍처 기반 API 개발
		- 서비스 성능 최적화 및 안정성 개선

		[자격 요건]
		- Java 또는 Kotlin 기반 서버 개발 경력 3년 이상
		- Spring Framework 기반 개발 경험
		- RDBMS, NoSQL 등 다양한 데이터베이스 사용 경험

		[우대 사항]
		- 대용량 트래픽 처리 경험
		- MSA 환경 개발 경험
		- Kafka, Redis 등 미들웨어 사용 경험

		[기술 스택]
		Java, Kotlin, Spring Boot, MySQL, Redis, Kafka, Kubernetes
		""";

	private AiService aiService;

	@BeforeEach
	void setUp() {
		String apiKey = System.getenv("GEMINI_API_KEY");
		String apiUrl = System.getenv("GEMINI_API_URL") != null
			? System.getenv("GEMINI_API_URL")
			: "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

		AiProperties props = new AiProperties(
			null,
			null,
			new AiProperties.ProviderProperties(apiKey, apiUrl, "gemini-2.5-flash", 2, true),
			new AiProperties.RetryProperties(2, 1000),
			new AiProperties.TimeoutProperties(10_000, 30_000)
		);

		WebClient webClient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create()
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
					.responseTimeout(Duration.ofSeconds(30))
			))
			.build();

		GeminiProvider geminiProvider = new GeminiProvider(webClient, props);

		aiService = new AiService(List.of(geminiProvider), props, new PromptRenderer());
	}

	@Test
	@DisplayName("AiService - PromptTemplate으로 채용공고 분석 후 AiResponse 수신")
	void analyzeJobPosting_viaAiService() {
		String rendered = PromptTemplate.JOB_ANALYSIS_V1.getContent()
			.replace("{{rawText}}", KAKAO_JOB_POSTING);
		AiPromptRequest request = AiPromptRequest.of(rendered, Map.of());

		AiResponse response = aiService.complete(request);

		System.out.println("=== AiService 응답 ===");
		System.out.println("provider : " + response.provider());
		System.out.println("model    : " + response.model());
		System.out.println("content  :\n" + response.content());
		System.out.println("====================");

		assertThat(response.provider()).isEqualTo("gemini");
		assertThat(response.model()).isNotBlank();
		assertThat(response.content()).isNotBlank();
		assertThat(response.content()).contains("카카오");
	}
}
