package com.aigo.speech.answer.dto;

import java.util.UUID;

public record SubmitAnswerResponse(
	UUID answerUuid,
	boolean sessionCompleted
) {}
