package com.aigo.speech.jobposting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crawler")
public record PlaywrightProperties(
	int poolSize,
	int pageTimeoutMs,
	int navigationTimeoutMs,
	boolean headless,
	int maxTextLength
) {}
