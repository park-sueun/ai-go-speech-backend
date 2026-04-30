package com.aigo.speech.question.dto;

import java.util.UUID;

import com.aigo.speech.question.entity.InterviewQuestion;

public record InterviewQuestionResponse(
	UUID uuid,
	int sequenceOrder,
	String content
) {
	public static InterviewQuestionResponse from (InterviewQuestion question) {
		return new InterviewQuestionResponse(
			question.getUuid(),
			question.getSequenceOrder(),
			question.getContent()
		);
	}
}
