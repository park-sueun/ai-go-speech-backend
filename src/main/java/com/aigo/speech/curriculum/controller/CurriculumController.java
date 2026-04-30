package com.aigo.speech.curriculum.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.curriculum.dto.CurriculmResponse;
import com.aigo.speech.curriculum.service.CurriculmService;
import com.aigo.speech.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumController {
	private final CurriculmService curriculmService;

	@PostMapping // 면접 일정 등록 후 커리큘럼 생성 호출
	public ResponseEntity<ApiResponse<List<CurriculmResponse>>> generate (
		@AuthenticationPrincipal String userUuid,
		@RequestParam UUID jobPostingUuid
	) {
		List<CurriculmResponse> responses = curriculmService.generateCurriculum(userUuid, jobPostingUuid);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(responses));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<CurriculmResponse>>> getCurriculum (
		@AuthenticationPrincipal String userUuid,
		@RequestParam UUID jobPostingUuid
	) {
		List<CurriculmResponse> responses = curriculmService.getCurriculumsByJobPosting(userUuid, jobPostingUuid);
		return ResponseEntity.ok(ApiResponse.success(responses));
	}

	@GetMapping("/today")
	public ResponseEntity<ApiResponse<List<CurriculmResponse>>> getTodayCurriculums (
		@AuthenticationPrincipal String userUuid
	) {
		List<CurriculmResponse> responses = curriculmService.getTodayCurriculums(userUuid);
		return ResponseEntity.ok(ApiResponse.success(responses));
	}
	
}
