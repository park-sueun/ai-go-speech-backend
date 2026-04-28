package com.aigo.speech.jobposting.parser;

import org.springframework.stereotype.Component;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.prompt.PromptTemplate;
import com.aigo.speech.ai.service.AiService;
import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;
import com.aigo.speech.jobposting.exception.JobPostingParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobPostingParser implements JobPostingParser {

	private static final ObjectMapper MAPPER = new ObjectMapper()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private final AiService aiService;

	@Override
	public JobPostingAnalyzeResponse parse(String rawText) {
		// {{rawText}} 플레이스홀더를 직접 치환 후 빈 변수맵으로 AiService에 전달
		String rendered = PromptTemplate.JOB_ANALYSIS_V1.getContent()
			.replace("{{rawText}}", rawText);
		AiPromptRequest request = AiPromptRequest.of(rendered, java.util.Map.of());

		AiResponse aiResponse = aiService.complete(request);
		String content = aiResponse.content();

		try {
			String json = stripMarkdownBlock(content);
			return MAPPER.readValue(json, JobPostingAnalyzeResponse.class);
		} catch (Exception e) {
			log.error("[Parser] AI 응답 JSON 파싱 실패. provider={}", aiResponse.provider(), e);
			throw new JobPostingParseException("채용공고 파싱에 실패했습니다.", e);
		}
	}

	private String stripMarkdownBlock(String text) {
		if (text == null || text.isBlank()) {
			throw new JobPostingParseException("AI 응답이 비어있습니다.");
		}
		String trimmed = text.strip();
		if (trimmed.startsWith("```")) {
			int start = trimmed.indexOf('\n') + 1;
			int end = trimmed.lastIndexOf("```");
			if (start > 0 && end > start) {
				return trimmed.substring(start, end).strip();
			}
		}
		return trimmed;
	}
}
