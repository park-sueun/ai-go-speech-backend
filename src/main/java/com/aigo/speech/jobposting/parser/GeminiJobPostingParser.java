package com.aigo.speech.jobposting.parser;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;
import com.aigo.speech.jobposting.exception.JobPostingParseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiJobPostingParser implements JobPostingParser {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${gemini.api.key}")
	private String apiKey;

	@Value("${gemini.api.url}")
	private String apiUrl;

	@Value("${parser.max-text-length:6000}")
	private int maxTextLength;

	private static final String PROMPT_TEMPLATE = """
		아래는 채용 공고 페이지에서 수집한 텍스트입니다.
		다음 JSON 형식으로 정보를 추출해주세요.
		없는 항목은 빈 문자열 또는 빈 배열로 두세요.
		반드시 JSON만 응답하고 마크다운 코드블록(```)이나 다른 텍스트는 포함하지 마세요.
		
		{
		  "companyName": "회사명",
		  "position": "직무명",
		  "companyDescription": "회사 소개",
		  "mainTasks": ["주요 업무 항목1", "항목2"],
		  "requirements": ["필수 자격 요건 항목1", "항목2"],
		  "preferred": ["우대 사항 항목1", "항목2"],
		  "techStacks": ["기술스택1", "기술스택2"]
		}
		
		채용 공고 텍스트:
		%s
		""";

	@Override
	public JobPostingAnalyzeResponse parse (String rawText) {
		log.debug("[Gemini] 파싱 시작. textLength={}", rawText.length());

		String prompt = String.format(PROMPT_TEMPLATE, truncate(rawText));
		String requestUrl = String.format("%s?key=%s", apiUrl, apiKey);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> requestBody = buildRequestBody(prompt);

		int maxRetries = 2;
		int retryDelayMs = 10_000;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				ResponseEntity<Map> response = restTemplate.exchange(
					requestUrl,
					HttpMethod.POST,
					new HttpEntity<>(requestBody, headers),
					Map.class
				);

				String json = extractTextFromResponse(response.getBody());
				log.debug("[Gemini] 응답 수신. json={}", json);

				return objectMapper.readValue(json, JobPostingAnalyzeResponse.class);

			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().value() == 429 && attempt < maxRetries) {
					log.warn("[Gemini] 429 Too Many Requests. {}초 후 재시도 ({}/{})", retryDelayMs / 1000, attempt, maxRetries - 1);
					try {
						Thread.sleep(retryDelayMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new JobPostingParseException("요청이 중단되었습니다.");
					}
					continue;
				}
				log.error("[Gemini] API 오류. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
				if (e.getStatusCode().value() == 429) {
					throw new JobPostingParseException("현재 AI 서비스 요청이 많습니다. 잠시 후 다시 시도해주세요.");
				}
				throw new JobPostingParseException("Gemini API 호출 실패: " + e.getStatusCode());
			} catch (Exception e) {
				log.error("[Gemini] 파싱 중 오류 발생: {}", e.getMessage());
				throw new JobPostingParseException("채용 정보 파싱 실패");
			}
		}
		throw new JobPostingParseException("채용 정보 파싱 실패");
	}

	private Map<String, Object> buildRequestBody (String prompt) {
		return Map.of(
			"contents", List.of(
				Map.of(
					"parts", List.of(
						Map.of("text", prompt)
					)
				)
			),
			"generationConfig", Map.of(
				"temperature", 0.1,          // 일관된 JSON 출력을 위해 낮게
				"maxOutputTokens", 4096
			)
		);
	}

	@SuppressWarnings("unchecked")
	private String extractTextFromResponse (Map<?, ?> responseBody) {
		try {
			List<Map<?, ?>> candidates = (List<Map<?, ?>>)responseBody.get("candidates");
			Map<?, ?> content = (Map<?, ?>)candidates.get(0).get("content");
			List<Map<?, ?>> parts = (List<Map<?, ?>>)content.get("parts");
			String raw = (String)parts.get(0).get("text");

			// 혹시 Gemini가 ```json ... ``` 감싸서 줄 경우 제거
			return raw.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
		} catch (Exception e) {
			throw new JobPostingParseException("Gemini 응답 구조 파싱 실패");
		}
	}

	private String truncate (String text) {
		return text.length() > maxTextLength
			? text.substring(0, maxTextLength)
			: text;
	}

}
