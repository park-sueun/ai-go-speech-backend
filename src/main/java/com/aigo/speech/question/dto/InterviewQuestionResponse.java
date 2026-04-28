package com.aigo.speech.question.dto;

import java.util.UUID;

public record InterviewQuestionResponse(
	UUID uuid,
	int sequenceOrder,
	String content
) {}
