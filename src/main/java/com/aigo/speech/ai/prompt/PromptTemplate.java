package com.aigo.speech.ai.prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

public enum PromptTemplate {

	JOB_ANALYSIS_V1("prompts/job_analysis_v1.txt"),
	INTERVIEW_QUESTION_V1("prompts/interview_question_v1.txt"),
	ANSWER_ANALYSIS_V1("prompts/answer_analysis_v1.txt"),
	SESSION_REPORT_V1("prompts/session_report_v1.txt"),
	COMBINED_ANALYSIS_V1("prompts/combined_analysis_v1.txt"),
	CURRICULUM_V1("prompts/curriculum_v1.txt");

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
