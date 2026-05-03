package com.aigo.speech.answer.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.aigo.speech.answer.dto.AnswerSubmittedEvent;
import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.answer.repository.InterviewAnswerRepository;
import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.interview.service.InterviewSessionService;
import com.aigo.speech.question.dto.SubmitAnswerRequest;
import com.aigo.speech.question.dto.SubmitAnswerRequest.TimePeriod;
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
			serializePeriods(request.getSilencePeriods()),
			serializePeriods(request.getSpeechPeriods())
		));

		log.info("[Answer] 답변 제출 완료. sessionUuid={}, questionUuid={}", session.getUuid(), question.getUuid());

		int silenceCount = request.getSilencePeriods() == null ? 0 : request.getSilencePeriods().size();
		int totalSilenceDuration = sumDurations(request.getSilencePeriods());
		int answerDuration = sumDurations(request.getSpeechPeriods());

		// 트랜잭션 커밋 후 AnswerScoreProcessor가 @TransactionalEventListener(AFTER_COMMIT)로 수신
		eventPublisher.publishEvent(new AnswerSubmittedEvent(
			answer.getId(),
			silenceCount,
			totalSilenceDuration,
			answerDuration
		));

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
					sseEmitterService.complete(sessionUuid);
				}
			});
			log.info("[Answer] 모든 답변 제출 완료. sessionUuid={}", session.getUuid());
		}

		return new SubmitAnswerResponse(answer.getUuid());
	}

	private String serializePeriods(List<TimePeriod> periods) {
		if (periods == null || periods.isEmpty()) return null;
		return periods.stream()
			.map(p -> "{\"startMs\":" + p.startMs() + ",\"endMs\":" + p.endMs() + "}")
			.collect(Collectors.joining(",", "[", "]"));
	}

	private int sumDurations(List<TimePeriod> periods) {
		if (periods == null) return 0;
		return periods.stream().mapToInt(p -> p.endMs() - p.startMs()).sum();
	}
}
