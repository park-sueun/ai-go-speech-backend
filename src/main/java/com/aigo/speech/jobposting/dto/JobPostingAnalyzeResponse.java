package com.aigo.speech.jobposting.dto;

import java.util.List;

public record JobPostingAnalyzeResponse(
	String companyName,
	String companyDescription,
	String position,
	List<String> responsibilities,
	List<String> requiredSkills,
	List<String> preferredSkills,
	String requiredExperience
) {}
