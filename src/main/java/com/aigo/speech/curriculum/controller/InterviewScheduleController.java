package com.aigo.speech.curriculum.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.curriculum.dto.CompletedJobPostingResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleFromJobPostingRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleListResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleUpdateRequest;
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
	public ResponseEntity<ApiResponse<InterviewScheduleResponse>> registerSchedule (
		Authentication authentication,
		@RequestBody @Valid InterviewScheduleFromJobPostingRequest request
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(scheduleService.registerFromJobPosting(userUuid, request)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<InterviewScheduleListResponse>>> getSchedules (
		Authentication authentication,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(scheduleService.getSchedules(userUuid, from)));
	}

	@GetMapping("/job-postings")
	public ResponseEntity<ApiResponse<List<CompletedJobPostingResponse>>> getCompletedJobPostings (
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(scheduleService.getCompletedJobPostings(userUuid)));
	}

	@GetMapping("/{scheduleUuid}")
	public ResponseEntity<ApiResponse<InterviewScheduleResponse>> getSchedule (
		Authentication authentication,
		@PathVariable UUID scheduleUuid
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(scheduleService.getSchedule(userUuid, scheduleUuid)));
	}

	@PutMapping("/{scheduleUuid}")
	public ResponseEntity<ApiResponse<InterviewScheduleResponse>> updateSchedule (
		Authentication authentication,
		@PathVariable UUID scheduleUuid,
		@RequestBody @Valid InterviewScheduleUpdateRequest request
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(scheduleService.updateSchedule(userUuid, scheduleUuid, request)));
	}

	@DeleteMapping("/{scheduleUuid}")
	public ResponseEntity<ApiResponse<Void>> deleteSchedule (
		Authentication authentication,
		@PathVariable UUID scheduleUuid
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		scheduleService.deleteSchedule(userUuid, scheduleUuid);
		return ResponseEntity.ok(ApiResponse.success(null));
	}
}
