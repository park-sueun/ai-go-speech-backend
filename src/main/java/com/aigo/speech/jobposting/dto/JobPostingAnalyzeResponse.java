package com.aigo.speech.jobposting.dto;

import java.util.List;

public record JobPostingAnalyzeResponse(
	String companyName,
	String position,
	String companyDescription,
	List<String> mainTasks,
	List<String> requirements,
	List<String> preferred,
	List<String> techStacks
) {
}
