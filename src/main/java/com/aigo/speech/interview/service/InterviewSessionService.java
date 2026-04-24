package com.aigo.speech.interview.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.interview.dto.CreateSessionRequest;
import com.aigo.speech.interview.dto.InterviewQuestionResponse;
import com.aigo.speech.interview.dto.InterviewSessionResponse;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.repository.InterviewQuestionRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewSessionService {

	private final InterviewSessionRepository sessionRepository;
	private final InterviewQuestionRepository questionRepository;
	private final UserRepository userRepository;
	private final JobPostingRepository jobPostingRepository;
	private final SseEmitterService sseEmitterService;
	private final InterviewQuestionGenerationService questionGenerationService;

	@Transactional
	public InterviewSessionResponse createSession(String userUuidStr, CreateSessionRequest request) {
		User user = userRepository.findByUuid(UUID.fromString(userUuidStr))
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		JobPosting jobPosting = null;
		if (request.getJobPostingUuid() != null) {
			jobPosting = jobPostingRepository.findByUuid(request.getJobPostingUuid())
				.orElseThrow(() -> new InterviewSessionNotFoundException("채용 공고를 찾을 수 없습니다."));
		}

		InterviewSession session = new InterviewSession(
			user, jobPosting, request.isRetry(), request.getInterviewDate()
		);
		session = sessionRepository.save(session);

		log.info("[Interview] Session created. uuid={}", session.getUuid());

		questionGenerationService.generateQuestionsAsync(session.getId(), request.getJobPostingContext());

		return toResponse(session, null);
	}

	public SseEmitter subscribeToSession(UUID sessionUuid) {
		findByUuid(sessionUuid);
		return sseEmitterService.register(sessionUuid);
	}

	public InterviewSessionResponse getSession(UUID sessionUuid) {
		InterviewSession session = findByUuid(sessionUuid);

		List<InterviewQuestionResponse> questions = questionRepository
			.findBySessionOrderBySequenceOrderAsc(session)
			.stream()
			.map(q -> new InterviewQuestionResponse(q.getUuid(), q.getSequenceOrder(), q.getContent()))
			.toList();

		return toResponse(session, questions.isEmpty() ? null : questions);
	}

	public InterviewSession findByUuid(UUID uuid) {
		return sessionRepository.findByUuid(uuid)
			.orElseThrow(() -> new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다."));
	}

	private InterviewSessionResponse toResponse(InterviewSession session, List<InterviewQuestionResponse> questions) {
		return new InterviewSessionResponse(
			session.getUuid(),
			session.getStatus().name(),
			session.getInterviewDate(),
			session.getStartedAt(),
			session.getEndedAt(),
			session.getCreatedAt(),
			questions
		);
	}
}
