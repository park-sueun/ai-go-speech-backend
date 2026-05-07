package com.aigo.speech.ranking.event;

import java.util.UUID;

public record InterviewCompletedEvent(
	UUID userUuid,
	int totalScore,
	String jobTitle
) {
}
