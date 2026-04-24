package com.aigo.speech.interview.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record JobPostingContext(
	@NotBlank String companyName,
	@NotBlank String jobName,
	List<String> mainTasks,
	List<String> requirements,
	List<String> preferred,
	List<String> techStacks
) {}
