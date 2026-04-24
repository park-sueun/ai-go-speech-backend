package com.aigo.speech.interview.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.aigo.speech.interview.dto.JobPostingContext;
import com.aigo.speech.interview.entity.InterviewQuestion;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewQuestionRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionGenerationService {

	private final InterviewSessionRepository sessionRepository;
	private final InterviewQuestionRepository questionRepository;
	private final SseEmitterService sseEmitterService;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${gemini.api.key}")
	private String apiKey;

	@Value("${gemini.api.url}")
	private String apiUrl;

	private static final String PROMPT_TEMPLATE = """
		당신은 면접관입니다. 아래 채용 공고 정보를 바탕으로 면접 질문 5개를 생성해주세요.
		반드시 JSON 배열 형태로만 응답하고, 마크다운 코드블록(```)이나 다른 텍스트는 포함하지 마세요.
		없는 항목은 무시하고 직무와 요건에 집중해주세요.

		예시: ["질문1", "질문2", "질문3", "질문4", "질문5"]

		채용 정보:
		- 회사명: %s
		- 직무명: %s
		- 주요 업무: %s
		- 필수 요건: %s
		- 우대 사항: %s
		- 기술 스택: %s
		""";

	@Async("interviewExecutor")
	@Transactional
	public void generateQuestionsAsync(Long sessionId, JobPostingContext context) {
		InterviewSession session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다."));

		UUID sessionUuid = session.getUuid();

		try {
			List<String> questions = callGemini(context);

			for (int i = 0; i < questions.size(); i++) {
				questionRepository.save(new InterviewQuestion(session, questions.get(i), i + 1));
			}

			session.start();
			sessionRepository.save(session);

			log.info("[Interview] Questions generated. sessionId={}, count={}", sessionId, questions.size());

			int questionCount = questions.size();
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					sseEmitterService.sendEvent(sessionUuid, "QUESTIONS_READY",
						Map.of("sessionUuid", sessionUuid.toString(), "questionCount", questionCount));
					sseEmitterService.complete(sessionUuid);
				}
			});

		} catch (Exception e) {
			log.error("[Interview] Question generation failed. sessionId={}, error={}", sessionId, e.getMessage());
			sseEmitterService.sendEvent(sessionUuid, "ERROR", Map.of("message", "질문 생성에 실패했습니다."));
			sseEmitterService.complete(sessionUuid);
		}
	}

	private List<String> callGemini(JobPostingContext ctx) {
		String prompt = String.format(PROMPT_TEMPLATE,
			ctx.companyName(),
			ctx.jobName(),
			join(ctx.mainTasks()),
			join(ctx.requirements()),
			join(ctx.preferred()),
			join(ctx.techStacks())
		);

		String requestUrl = String.format("%s?key=%s", apiUrl, apiKey);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = Map.of(
			"contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
			"generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024)
		);

		int maxRetries = 2;
		int retryDelayMs = 10_000;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				ResponseEntity<Map> response = restTemplate.exchange(
					requestUrl, HttpMethod.POST,
					new HttpEntity<>(body, headers), Map.class
				);
				String json = extractText(response.getBody());
				log.debug("[Interview] Gemini response. json={}", json);
				return objectMapper.readValue(json, new TypeReference<List<String>>() {});

			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().value() == 429 && attempt < maxRetries) {
					log.warn("[Interview] Gemini 429. {}초 후 재시도 ({}/{})", retryDelayMs / 1000, attempt, maxRetries - 1);
					try {
						Thread.sleep(retryDelayMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("요청이 중단되었습니다.");
					}
					continue;
				}
				throw new RuntimeException("Gemini API 오류: " + e.getStatusCode());
			} catch (Exception e) {
				throw new RuntimeException("질문 생성 파싱 실패: " + e.getMessage());
			}
		}
		throw new RuntimeException("질문 생성 실패");
	}

	@SuppressWarnings("unchecked")
	private String extractText(Map<?, ?> body) {
		try {
			List<Map<?, ?>> candidates = (List<Map<?, ?>>)body.get("candidates");
			Map<?, ?> content = (Map<?, ?>)candidates.get(0).get("content");
			List<Map<?, ?>> parts = (List<Map<?, ?>>)content.get("parts");
			String raw = (String)parts.get(0).get("text");
			return raw.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
		} catch (Exception e) {
			throw new RuntimeException("Gemini 응답 구조 파싱 실패");
		}
	}

	private String join(List<String> list) {
		if (list == null || list.isEmpty()) {
			return "";
		}
		return String.join(", ", list);
	}
}
