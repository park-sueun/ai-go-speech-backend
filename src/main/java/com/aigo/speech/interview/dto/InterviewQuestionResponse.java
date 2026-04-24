package com.aigo.speech.interview.dto;

import java.util.UUID;

public record InterviewQuestionResponse(
	UUID uuid,
	int sequenceOrder,
	String content
) {}
