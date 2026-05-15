package com.aigo.speech.curriculum.dto;

import java.util.UUID;

public record CurriculumGenerationRequest(
	UUID scheduleUuid,
	String companyName,
	String position,
	String requiredSkills,
	String preferredSkills,
	String fillerScore,
	String logicScore,
	String silenceScore,
	String fillerTarget,
	String logicTarget,
	String silenceTarget
) {}
