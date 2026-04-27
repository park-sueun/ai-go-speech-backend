package com.aigo.speech.ai.prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

public enum PromptTemplate {

	JOB_POSTING_PARSE_V1("prompts/job_posting_parse_v1.txt");

	private final String path;
	private volatile String content;

	PromptTemplate(String path) {
		this.path = path;
	}

	public String getContent() {
		if (content == null) {
			synchronized (this) {
				if (content == null) {
					content = load(path);
				}
			}
		}
		return content;
	}

	private static String load(String path) {
		try {
			return new ClassPathResource(path)
				.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("프롬프트 파일을 읽을 수 없습니다: " + path, e);
		}
	}
}
