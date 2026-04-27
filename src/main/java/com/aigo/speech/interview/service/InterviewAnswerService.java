package com.aigo.speech.interview.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.interview.dto.SubmitAnswerRequest;
import com.aigo.speech.interview.dto.SubmitAnswerResponse;
import com.aigo.speech.interview.entity.AnswerScore;
import com.aigo.speech.interview.entity.InterviewAnswer;
import com.aigo.speech.interview.entity.InterviewQuestion;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.interview.exception.QuestionNotFoundException;
import com.aigo.speech.interview.repository.AnswerScoreRepository;
import com.aigo.speech.interview.repository.InterviewAnswerRepository;
import com.aigo.speech.interview.repository.InterviewQuestionRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAnswerService {

	private final InterviewSessionRepository sessionRepository;
	private final InterviewQuestionRepository questionRepository;
	private final InterviewAnswerRepository answerRepository;
	private final AnswerScoreRepository answerScoreRepository;
	private final SseEmitterService sseEmitterService;
	private final InterviewSessionService sessionService;

	@Transactional
	public SubmitAnswerResponse submitAnswer(String userUuidStr, UUID sessionUuid, UUID questionUuid, SubmitAnswerRequest request) {
		InterviewSession session = sessionService.findByUuid(sessionUuid);
		sessionService.validateOwnership(session, userUuidStr);

		if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
			throw new InvalidSessionStatusException("진행 중인 면접 세션이 아닙니다.");
		}

		InterviewQuestion question = questionRepository.findByUuid(questionUuid)
			.orElseThrow(() -> new QuestionNotFoundException("질문을 찾을 수 없습니다."));

		if (!question.getSession().getId().equals(session.getId())) {
			throw new QuestionNotFoundException("해당 세션의 질문이 아닙니다.");
		}

		if (answerRepository.existsByQuestion(question)) {
			throw new InvalidSessionStatusException("이미 답변이 제출된 질문입니다.");
		}

		InterviewAnswer answer = answerRepository.save(new InterviewAnswer(
			question,
			request.getAudioUrl(),
			request.getSttText(),
			request.getDuration(),
			request.getSilenceIntervalsJson(),
			request.getAnswerStartedAt(),
			request.getAnswerEndedAt()
		));

		answerScoreRepository.save(new AnswerScore(
			answer,
			request.getSilenceCount(),
			request.getTotalSilenceDuration(),
			request.getAnswerDuration()
		));

		log.info("[Interview] Answer submitted. sessionUuid={}, questionUuid={}", sessionUuid, questionUuid);

		long totalQuestions = questionRepository.countBySession(session);
		long totalAnswers = answerRepository.countBySession(session);

		boolean sessionCompleted = false;
		if (totalAnswers >= totalQuestions) {
			session.complete();
			sessionRepository.save(session);
			sessionCompleted = true;

			sseEmitterService.sendEvent(sessionUuid, "SESSION_COMPLETED",
				Map.of("sessionUuid", sessionUuid.toString()));
			sseEmitterService.complete(sessionUuid);

			log.info("[Interview] Session completed. sessionUuid={}", sessionUuid);
		}

		return new SubmitAnswerResponse(answer.getUuid(), sessionCompleted);
	}
}
