package com.aigo.speech.curriculum.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.service.CurriculmService;
import com.aigo.speech.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumController {
	private final CurriculmService curriculmService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<CurriculumResponse>>> getCurriculum (
		Authentication authentication,
		@RequestParam UUID scheduleUuid
	) {
		UUID userUuid = UUID.fromString(authentication.getName());

		List<CurriculumResponse> responses = curriculmService.getCurriculumsBySchedule(userUuid, scheduleUuid);
		return ResponseEntity.ok(ApiResponse.success(responses));
	}

	@GetMapping("/today")
	public ResponseEntity<ApiResponse<List<CurriculumResponse>>> getTodayCurriculums (
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());

		List<CurriculumResponse> responses = curriculmService.getTodayCurriculums(userUuid.toString());
		return ResponseEntity.ok(ApiResponse.success(responses));
	}

}
