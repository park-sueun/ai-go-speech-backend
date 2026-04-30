package com.aigo.speech.interview.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.interview.dto.CreateSessionRequest;
import com.aigo.speech.interview.dto.InterviewSessionResponse;
import com.aigo.speech.interview.service.InterviewSessionService;
import com.aigo.speech.question.dto.InterviewQuestionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interview-sessions")
@RequiredArgsConstructor
public class InterviewSessionController {

	private final InterviewSessionService sessionService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<InterviewSessionResponse>>> getSessions(
		@AuthenticationPrincipal String userUuid
	) {
		List<InterviewSessionResponse> response = sessionService.getSessions(userUuid);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

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
		return sessionService.subscribeToSession(userUuid, uuid);
	}

	@PatchMapping("/{uuid}/start")
	public ResponseEntity<ApiResponse<InterviewSessionResponse>> startSession(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid
	) {
		InterviewSessionResponse response = sessionService.startSession(userUuid, uuid);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PatchMapping("/{uuid}/complete")
	public ResponseEntity<ApiResponse<InterviewSessionResponse>> completeSession(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid
	) {
		InterviewSessionResponse response = sessionService.completeSession(userUuid, uuid);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/{uuid}/questions")
	public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> getQuestions(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid,
		@RequestParam(required = false) Integer sequenceOrder
	) {
		List<InterviewQuestionResponse> response = sessionService.getQuestions(userUuid, uuid, sequenceOrder);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/{uuid}")
	public ResponseEntity<ApiResponse<InterviewSessionResponse>> getSession(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid
	) {
		InterviewSessionResponse response = sessionService.getSession(userUuid, uuid);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

}
