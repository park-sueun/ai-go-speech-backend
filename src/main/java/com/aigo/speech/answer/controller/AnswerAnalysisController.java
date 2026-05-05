package com.aigo.speech.answer.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.answer.dto.AnswerAnalysisResponse;
import com.aigo.speech.answer.service.AnswerAnalysisService;
import com.aigo.speech.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/answer-analyses")
@RequiredArgsConstructor
public class AnswerAnalysisController {

	private final AnswerAnalysisService answerAnalysisService;

	@GetMapping("/{uuid}")
	public ResponseEntity<ApiResponse<AnswerAnalysisResponse>> getAnalysis(
		@AuthenticationPrincipal String userUuid,
		@PathVariable UUID uuid
	) {
		return ResponseEntity.ok(ApiResponse.success(
			answerAnalysisService.getByUuid(uuid, userUuid)
		));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<AnswerAnalysisResponse>>> getAnalysesBySession(
		@AuthenticationPrincipal String userUuid,
		@RequestParam UUID sessionUuid
	) {
		return ResponseEntity.ok(ApiResponse.success(
			answerAnalysisService.getBySessionUuid(sessionUuid, userUuid)
		));
	}
}
