package com.aigo.speech.answer.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.answer.entity.AnswerScore;
import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.answer.repository.AnswerScoreRepository;
import com.aigo.speech.answer.repository.InterviewAnswerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerScoreService {

	private final InterviewAnswerRepository answerRepository;
	private final AnswerScoreRepository answerScoreRepository;

	@Async("interviewExecutor")
	@Transactional
	public void saveScoreAsync(Long answerId, Integer silenceCount,
		Integer totalSilenceDuration, Integer answerDuration) {

		InterviewAnswer answer = answerRepository.findById(answerId)
			.orElseThrow(() -> new IllegalStateException("답변을 찾을 수 없습니다: " + answerId));

		answerScoreRepository.save(new AnswerScore(answer, silenceCount, totalSilenceDuration, answerDuration));

		log.info("[AnswerScore] 점수 저장 완료. answerId={}", answerId);
	}
}
