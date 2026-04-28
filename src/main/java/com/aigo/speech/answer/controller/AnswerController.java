package com.aigo.speech.answer.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.answer.dto.SubmitAnswerRequest;
import com.aigo.speech.answer.dto.SubmitAnswerResponse;
import com.aigo.speech.answer.service.InterviewAnswerService;
import com.aigo.speech.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interview-sessions")
@RequiredArgsConstructor
public class AnswerController {

	private final InterviewAnswerService answerService;

	@PostMapping("/{uuid}/questions/{questionUuid}/answers")
	public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid,
		@PathVariable UUID questionUuid,
		@RequestBody @Valid SubmitAnswerRequest request
	) {
		SubmitAnswerResponse response = answerService.submitAnswer(userUuid, uuid, questionUuid, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
