package com.aigo.speech.interview.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.interview.dto.CreateSessionRequest;
import com.aigo.speech.interview.dto.InterviewSessionResponse;
import com.aigo.speech.interview.dto.SubmitAnswerRequest;
import com.aigo.speech.interview.dto.SubmitAnswerResponse;
import com.aigo.speech.interview.service.InterviewAnswerService;
import com.aigo.speech.interview.service.InterviewSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interview-sessions")
@RequiredArgsConstructor
public class InterviewSessionController {

	private final InterviewSessionService sessionService;
	private final InterviewAnswerService answerService;

	@PostMapping
	public ResponseEntity<ApiResponse<InterviewSessionResponse>> createSession(
		@AuthenticationPrincipal String userUuid,
		@RequestBody @Valid CreateSessionRequest request
	) {
		InterviewSessionResponse response = sessionService.createSession(userUuid, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@GetMapping(value = "/{uuid}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamSession(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid
	) {
		return sessionService.subscribeToSession(uuid);
	}

	@GetMapping("/{uuid}")
	public ResponseEntity<ApiResponse<InterviewSessionResponse>> getSession(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid
	) {
		InterviewSessionResponse response = sessionService.getSession(uuid);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PostMapping("/{uuid}/questions/{questionUuid}/answers")
	public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid,
		@PathVariable UUID questionUuid,
		@RequestBody @Valid SubmitAnswerRequest request
	) {
		SubmitAnswerResponse response = answerService.submitAnswer(uuid, questionUuid, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
