package com.aigo.speech.ai.config;

import java.time.Duration;
import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

	@Bean("aiWebClient")
	public WebClient aiWebClient(AiProperties props) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.timeout().connectMs())
			.responseTimeout(Duration.ofMillis(props.timeout().readMs()));
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();
	}

	@Bean("aiExecutor")
	public Executor aiExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("ai-worker-");
		executor.initialize();
		return executor;
	}
}
