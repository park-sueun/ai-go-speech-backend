package com.aigo.speech.interview.dto;

import java.util.UUID;

public record SubmitAnswerResponse(
	UUID answerUuid,
	boolean sessionCompleted
) {}
