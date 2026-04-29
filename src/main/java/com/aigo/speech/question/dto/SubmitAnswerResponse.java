package com.aigo.speech.question.dto;

import java.util.UUID;

public record SubmitAnswerResponse(
	UUID answerUuid,
	boolean sessionCompleted
) {
}
