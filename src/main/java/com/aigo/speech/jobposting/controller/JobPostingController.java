package com.aigo.speech.jobposting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.jobposting.dto.JobPostingAnalyzeRequest;
import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;
import com.aigo.speech.jobposting.service.JobPostingAnalyzeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

	private final JobPostingAnalyzeService analyzeService;

	@PostMapping("/analyze")
	public ResponseEntity<ApiResponse<JobPostingAnalyzeResponse>> analyze (
		@RequestBody @Valid JobPostingAnalyzeRequest request
	) {
		JobPostingAnalyzeResponse response = analyzeService.analyze(request.getUrl());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
