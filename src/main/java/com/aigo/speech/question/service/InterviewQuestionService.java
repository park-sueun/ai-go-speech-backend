package com.aigo.speech.question.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.dto.AiResponse;
import com.aigo.speech.ai.prompt.PromptTemplate;
import com.aigo.speech.ai.service.AiService;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.repository.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

	private final InterviewSessionRepository sessionRepository;
	private final InterviewQuestionRepository questionRepository;
	private final SseEmitterService sseEmitterService;
	private final AiService aiService;
	private final ObjectMapper objectMapper;

	@Async("interviewExecutor")
	@Transactional
	public void generateQuestionsAsync(Long sessionId) {
		InterviewSession session = sessionRepository.findById(sessionId)
			.orElseThrow(() -> new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다."));

		UUID sessionUuid = session.getUuid();

		try {
			List<String> questions = generateQuestions(session);

			for (int i = 0; i < questions.size(); i++) {
				questionRepository.save(new InterviewQuestion(session, questions.get(i), i + 1));
			}

			log.info("[Question] 질문 생성 완료. sessionId={}, count={}", sessionId, questions.size());

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
			log.error("[Question] 질문 생성 실패. sessionId={}, error={}", sessionId, e.getMessage());
			sseEmitterService.sendEvent(sessionUuid, "ERROR", Map.of("message", "질문 생성에 실패했습니다."));
			sseEmitterService.complete(sessionUuid);
		}
	}

	private List<String> generateQuestions(InterviewSession session) {
		String previousQuestionsText = resolvePreviousQuestions(session);
		JobPosting jobPosting = session.getJobPosting();

		String template = PromptTemplate.INTERVIEW_QUESTION_V1.getContent()
			.replace("{{previousQuestions}}", previousQuestionsText);

		AiPromptRequest request = AiPromptRequest.of(
			template,
			Map.of(
				"companyName", orEmpty(jobPosting.getCompanyName()),
				"companyDescription", orEmpty(jobPosting.getCompanyDescription()),
				"position", orEmpty(jobPosting.getPosition()),
				"responsibilities", join(jobPosting.getResponsibilities()),
				"requiredSkills", join(jobPosting.getRequiredSkills()),
				"preferredSkills", join(jobPosting.getPreferredSkills()),
				"requiredExperience", orEmpty(jobPosting.getRequiredExperience())
			)
		);

		AiResponse response = aiService.complete(request);
		log.debug("[Question] AI 응답 수신. provider={}, length={}", response.provider(), response.content().length());

		try {
			return objectMapper.readValue(response.content(), new TypeReference<List<String>>() {});
		} catch (Exception e) {
			throw new RuntimeException("질문 응답 파싱 실패: " + e.getMessage());
		}
	}

	private String resolvePreviousQuestions(InterviewSession session) {
		if (!session.isRetry()) {
			return "없음";
		}
		List<InterviewSession> previousSessions = sessionRepository
			.findByUserAndJobPostingOrderByCreatedAtAsc(session.getUser(), session.getJobPosting());

		String questions = previousSessions.stream()
			.filter(s -> !s.getId().equals(session.getId()))
			.flatMap(s -> questionRepository.findBySessionOrderBySequenceOrderAsc(s).stream())
			.map(q -> "- " + q.getContent())
			.collect(Collectors.joining("\n"));

		return questions.isBlank() ? "없음" : questions;
	}

	private String join(List<String> list) {
		if (list == null || list.isEmpty()) return "";
		return String.join(", ", list);
	}

	private String orEmpty(String value) {
		return value != null ? value : "";
	}
}
