package com.aigo.speech.interview.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.aigo.speech.question.dto.InterviewQuestionResponse;

public record InterviewSessionResponse(
	UUID uuid,
	String status,
	LocalDate interviewDate,
	LocalDateTime startedAt,
	LocalDateTime endedAt,
	LocalDateTime createdAt,
	List<InterviewQuestionResponse> questions
) {}
