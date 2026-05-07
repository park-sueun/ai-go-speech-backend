package com.aigo.speech.interview.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aigo.speech.answer.repository.InterviewAnswerRepository;
import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.dto.CreateSessionRequest;
import com.aigo.speech.interview.dto.InterviewSessionResponse;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
import com.aigo.speech.question.dto.InterviewQuestionResponse;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.repository.InterviewQuestionRepository;
import com.aigo.speech.question.service.InterviewQuestionService;
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
	private final InterviewReportRepository reportRepository;
	private final InterviewQuestionRepository questionRepository;
	private final InterviewAnswerRepository answerRepository;
	private final UserRepository userRepository;
	private final JobPostingRepository jobPostingRepository;
	private final SseEmitterService sseEmitterService;
	private final InterviewQuestionService questionService;

	@Transactional
	public InterviewSessionResponse createSession (String userUuidStr, CreateSessionRequest request) {
		User user = userRepository.findByUuid(UUID.fromString(userUuidStr))
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		JobPosting jobPosting = jobPostingRepository.findByUuid(request.getJobPostingUuid())
			.orElseThrow(() -> new InterviewSessionNotFoundException("채용 공고를 찾을 수 없습니다."));

		InterviewSession session = new InterviewSession(
			user, jobPosting, request.isRetry(), request.getInterviewDate()
		);
		session = sessionRepository.save(session);

		log.info("[Interview] Session created. uuid={}", session.getUuid());

		/* AI 면접 질문 생성 (비동기) */
        Long sessionId = session.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                questionService.generateQuestionsAsync(sessionId);
            }
        });

		return InterviewSessionResponse.from(session, null);
	}

	public SseEmitter subscribeToSession (String userUuidStr, UUID sessionUuid) {
		InterviewSession session = findByUuid(sessionUuid);
		validateOwnership(session, userUuidStr);

		InterviewReport report = reportRepository.findBySession(session).orElse(null);
		if (report != null && report.getStatus() != InterviewReport.ReportStatus.GENERATING) {
			SseEmitter emitter = sseEmitterService.register(sessionUuid);
			if (report.getStatus() == InterviewReport.ReportStatus.COMPLETED) {
				sseEmitterService.sendEvent(sessionUuid, "SESSION_ANALYSIS_DONE",
					Map.of("sessionUuid", sessionUuid.toString()));
			} else {
				sseEmitterService.sendEvent(sessionUuid, "ERROR",
					Map.of("message", "리포트 생성에 실패했습니다."));
			}
			sseEmitterService.complete(sessionUuid);
			return emitter;
		}

		return sseEmitterService.register(sessionUuid);
	}

	public List<InterviewSessionResponse> getSessions (String userUuidStr) {
		User user = userRepository.findByUuid(UUID.fromString(userUuidStr))
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		return sessionRepository.findByUserOrderByCreatedAtDesc(user)
			.stream()
			.map(s -> InterviewSessionResponse.from(s, null))
			.toList();
	}

	@Transactional
	public InterviewSessionResponse startSession (String userUuidStr, UUID sessionUuid) {
		InterviewSession session = findByUuid(sessionUuid);
		validateOwnership(session, userUuidStr);
		session.start();
		return InterviewSessionResponse.from(session, null);
	}

	@Transactional
	public InterviewSessionResponse completeSession (String userUuidStr, UUID sessionUuid) {
		InterviewSession session = findByUuid(sessionUuid);
		validateOwnership(session, userUuidStr);

		long totalQuestions = questionRepository.countBySession(session);
		long totalAnswers = answerRepository.countBySession(session);

		if (totalAnswers < totalQuestions) {
			throw new InvalidSessionStatusException("모든 질문에 답변한 후 세션을 완료할 수 있습니다.");
		}

		session.complete();

		return InterviewSessionResponse.from(session, null);
	}

	@Transactional
	public InterviewSessionResponse abandonSession (String userUuidStr, UUID sessionUuid) {
		InterviewSession session = findByUuid(sessionUuid);
		validateOwnership(session, userUuidStr);
		session.abandon();
		return InterviewSessionResponse.from(session, null);
	}

	public List<InterviewQuestionResponse> getQuestions (String userUuidStr, UUID sessionUuid, Integer sequenceOrder) {
		InterviewSession session = findByUuid(sessionUuid);
		validateOwnership(session, userUuidStr);

		if (sequenceOrder != null) {
			InterviewQuestion question = questionRepository.findBySessionAndSequenceOrder(session, sequenceOrder)
				.orElseThrow(() -> new InterviewSessionNotFoundException("해당 순서의 질문을 찾을 수 없습니다."));
			return List.of(InterviewQuestionResponse.from(question));
		}

		return questionRepository.findBySessionOrderBySequenceOrderAsc(session)
			.stream()
			.map(InterviewQuestionResponse::from)
			.toList();
	}

	public InterviewSessionResponse getSession (String userUuidStr, UUID sessionUuid) {
		InterviewSession session = findByUuid(sessionUuid);
		validateOwnership(session, userUuidStr);

		List<InterviewQuestion> questions = questionRepository
			.findBySessionOrderBySequenceOrderAsc(session);

		return InterviewSessionResponse.from(session, questions.isEmpty() ? null : questions);
	}

	public InterviewSession findByUuid (UUID uuid) {
		return sessionRepository.findByUuid(uuid)
			.orElseThrow(() -> new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다."));
	}

	public void validateOwnership (InterviewSession session, String userUuidStr) {
		if (!session.getUser().getUuid().equals(UUID.fromString(userUuidStr))) {
			throw new InterviewSessionNotFoundException("면접 세션을 찾을 수 없습니다.");
		}
	}
}
