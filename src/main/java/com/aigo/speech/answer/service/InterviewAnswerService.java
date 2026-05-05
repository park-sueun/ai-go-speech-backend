package com.aigo.speech.answer.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.aigo.speech.answer.dto.AllAnswersSubmittedEvent;
import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.answer.repository.InterviewAnswerRepository;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.interview.service.InterviewSessionService;
import com.aigo.speech.question.dto.SubmitAnswerRequest;
import com.aigo.speech.question.dto.SubmitAnswerResponse;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.aigo.speech.question.exception.QuestionNotFoundException;
import com.aigo.speech.question.repository.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAnswerService {

	private final InterviewQuestionRepository questionRepository;
	private final InterviewAnswerRepository answerRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final SseEmitterService sseEmitterService;
	private final InterviewSessionService sessionService;

	@Transactional
	public SubmitAnswerResponse submitAnswer (String userUuidStr, UUID questionUuid, SubmitAnswerRequest request) {
		InterviewQuestion question = questionRepository.findByUuid(questionUuid)
			.orElseThrow(() -> new QuestionNotFoundException("질문을 찾을 수 없습니다."));

		InterviewSession session = question.getSession();
		sessionService.validateOwnership(session, userUuidStr);

		if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
			throw new InvalidSessionStatusException("진행 중인 면접 세션이 아닙니다.");
		}

		if (answerRepository.existsByQuestion(question)) {
			throw new InvalidSessionStatusException("이미 답변이 제출된 질문입니다.");
		}

		InterviewAnswer answer = answerRepository.save(new InterviewAnswer(
			question,
			request.getTotalElapsedMs(),
			request.getTranscript(),
			request.getFillerWords(),
			request.getSilencePeriods(),
			request.getSpeechPeriods()
		));

		log.info("[Answer] 답변 제출 완료. sessionUuid={}, questionUuid={}", session.getUuid(), question.getUuid());

		long totalQuestions = questionRepository.countBySession(session);
		long totalAnswers = answerRepository.countBySession(session);

		if (totalAnswers >= totalQuestions) {
			UUID sessionUuid = session.getUuid();
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit () {
					sseEmitterService.sendEvent(
						sessionUuid, "ALL_ANSWERS_SUBMITTED",
						Map.of("sessionUuid", sessionUuid.toString())
					);
					// SSE 연결은 분석 완료(SESSION_ANALYSIS_DONE) 후에 닫힘
				}
			});
			eventPublisher.publishEvent(new AllAnswersSubmittedEvent(session.getId()));
			log.info("[Answer] 모든 답변 제출 완료. sessionUuid={}", session.getUuid());
		}

		return new SubmitAnswerResponse(answer.getUuid());
	}
}