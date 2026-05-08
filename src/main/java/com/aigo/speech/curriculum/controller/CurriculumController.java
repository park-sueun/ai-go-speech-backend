package com.aigo.speech.curriculum.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.service.CurriculumService;
import com.aigo.speech.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumController {
	private final CurriculumService curriculumService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<CurriculumResponse>>> getCurriculums (
		Authentication authentication,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		UUID userUuid = UUID.fromString(authentication.getName());

		/* 오늘의 스케줄 - 홈 화면 기준 우측 상단 */
		LocalDate targetDate = (date != null) ? date : LocalDate.now();
		List<CurriculumResponse> responses = curriculumService.getCurriculumsByDate(userUuid, targetDate);
		return ResponseEntity.ok(ApiResponse.success(responses));
	}

}
