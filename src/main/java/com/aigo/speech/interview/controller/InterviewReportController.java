package com.aigo.speech.interview.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.interview.dto.InterviewReportDetailResponse;
import com.aigo.speech.interview.dto.InterviewReportSummaryResponse;
import com.aigo.speech.interview.service.InterviewReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interview-reports")
@RequiredArgsConstructor
public class InterviewReportController {

	private final InterviewReportService interviewReportService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<InterviewReportSummaryResponse>>> getReportList(
		@AuthenticationPrincipal String userUuid
	) {
		return ResponseEntity.ok(ApiResponse.success(
			interviewReportService.getReportList(userUuid)
		));
	}

	@GetMapping("/{sessionUuid}")
	public ResponseEntity<ApiResponse<InterviewReportDetailResponse>> getReportDetail(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID sessionUuid
	) {
		return ResponseEntity.ok(ApiResponse.success(
			interviewReportService.getReportDetail(sessionUuid, userUuid)
		));
	}
}
