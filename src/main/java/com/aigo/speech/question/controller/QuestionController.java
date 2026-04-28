package com.aigo.speech.question.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.answer.service.InterviewAnswerService;
import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.question.dto.SubmitAnswerRequest;
import com.aigo.speech.question.dto.SubmitAnswerResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

	private final InterviewAnswerService answerService;

	@PostMapping("/{uuid}/answers")
	public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer (
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid,
		@RequestBody @Valid SubmitAnswerRequest request
	) {
		SubmitAnswerResponse response = answerService.submitAnswer(userUuid, uuid, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
