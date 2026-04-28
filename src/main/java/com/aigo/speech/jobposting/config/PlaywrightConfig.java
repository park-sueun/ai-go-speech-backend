package com.aigo.speech.jobposting.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(PlaywrightProperties.class)
public class PlaywrightConfig {

	@Bean("crawlerExecutor")
	public Executor crawlerExecutor(PlaywrightProperties props) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(props.poolSize());
		executor.setMaxPoolSize(props.poolSize());
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("crawler-");
		executor.initialize();
		return executor;
	}
}
