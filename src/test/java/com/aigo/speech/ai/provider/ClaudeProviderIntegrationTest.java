package com.aigo.speech.ai.provider;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.aigo.speech.ai.client.ClaudeProvider;
import com.aigo.speech.ai.config.AiProperties;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@EnabledIfEnvironmentVariable(named = "CLAUDE_API_KEY", matches = ".+")
class ClaudeProviderIntegrationTest {

	private static final String KAKAO_JOB_POSTING = """
		카카오 서버 개발자 채용
		
		[담당 업무]
		- 카카오 서비스를 위한 백엔드 서버 개발 및 운영
		- 대용량 트래픽 처리를 위한 분산 시스템 설계
		
		[자격 요건]
		- Java 또는 Kotlin 기반 서버 개발 경력 3년 이상
		- Spring Framework 기반 개발 경험
		
		[기술 스택]
		Java, Kotlin, Spring Boot, MySQL, Redis, Kafka, Kubernetes
		""";

	private ClaudeProvider claudeProvider;

	@BeforeEach
	void setUp () {
		String apiKey = System.getenv("CLAUDE_API_KEY");

		AiProperties props = new AiProperties(
			null,
			new AiProperties.ProviderProperties(
				apiKey, "https://api.anthropic.com/v1/messages", List.of("claude-haiku-4-5"), 1, true),
			null,
			new AiProperties.RetryProperties(1, 1000, 500L),
			new AiProperties.TimeoutProperties(10_000, 30_000)
		);

		WebClient webClient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create()
					.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
					.responseTimeout(Duration.ofSeconds(30))
			))
			.build();

		claudeProvider = new ClaudeProvider(webClient, props);
	}

	@Test
	@DisplayName("카카오 서버 개발자 공고 분석 - Claude API 직접 호출")
	void analyzeKakaoJobPosting () {
		String userPrompt = """
			아래 채용 공고에서 다음 정보를 JSON으로 추출해주세요:
			companyName, position, mainTasks(배열), requirements(배열), preferred(배열), techStacks(배열)
			JSON만 응답하세요.
			
			%s
			""".formatted(KAKAO_JOB_POSTING);

		String response = claudeProvider.complete("", userPrompt);

		System.out.println("=== Claude 응답 ===");
		System.out.println(response);
		System.out.println("==================");

		assertThat(response).isNotBlank();
		assertThat(response).contains("카카오");
	}
}
