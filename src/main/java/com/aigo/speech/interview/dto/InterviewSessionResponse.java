package com.aigo.speech.interview.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.question.dto.InterviewQuestionResponse;
import com.aigo.speech.question.entity.InterviewQuestion;
import com.fasterxml.jackson.annotation.JsonInclude;

public record InterviewSessionResponse(
	UUID uuid,
	String status,
	boolean isRetry,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Integer attemptNumber,
	LocalDate interviewDate,
	LocalDateTime startedAt,
	LocalDateTime endedAt,
	LocalDateTime createdAt,

	@JsonInclude(JsonInclude.Include.NON_NULL)
	List<InterviewQuestionResponse> questions
) {
	public static InterviewSessionResponse from (InterviewSession session, List<InterviewQuestion> questions) {
		return new InterviewSessionResponse(
			session.getUuid(),
			session.getStatus().name(),
			session.isRetry(),
			session.getAttemptNumber(),
			session.getInterviewDate(),
			session.getStartedAt(),
			session.getEndedAt(),
			session.getCreatedAt(),
			questions == null ? null
				: questions.stream()
				.map(InterviewQuestionResponse::from)
				.toList()
		);
	}
}
