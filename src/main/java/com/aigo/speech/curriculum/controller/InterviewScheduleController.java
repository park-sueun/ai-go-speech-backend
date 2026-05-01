package com.aigo.speech.curriculum.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.curriculum.dto.InterviewScheduleRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.service.InterviewScheduleService;
import com.aigo.speech.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class InterviewScheduleController {
	private final InterviewScheduleService scheduleService;

	@PostMapping
	public ResponseEntity<ApiResponse<InterviewScheduleResponse>> registerInterviewSchedule (
		Authentication authentication,
		@RequestBody @Valid InterviewScheduleRequest request
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(scheduleService.register(userUuid, request)));
	}
}
