package com.aigo.speech.answer.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
	private final AnswerScoreService answerScoreService;
	private final InterviewSessionService sessionService;

	@Transactional
	public SubmitAnswerResponse submitAnswer(String userUuidStr, UUID questionUuid, SubmitAnswerRequest request) {
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
			request.getAudioUrl(),
			request.getSttText(),
			request.getDuration(),
			request.getSilenceIntervalsJson(),
			request.getAnswerStartedAt(),
			request.getAnswerEndedAt()
		));

		log.info("[Answer] 답변 제출 완료. sessionUuid={}, questionUuid={}", session.getUuid(), question.getUuid());

		answerScoreService.saveScoreAsync(answer.getId(),
			request.getSilenceCount(), request.getTotalSilenceDuration(), request.getAnswerDuration());

		return new SubmitAnswerResponse(answer.getUuid());
	}
}
