package com.aigo.speech.ranking.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.ranking.dto.RankingListResponse;
import com.aigo.speech.ranking.service.RankingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {
	private final RankingService rankingService;

	@GetMapping
	public ResponseEntity<ApiResponse<RankingListResponse>> getRankingList (
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(rankingService.getRankings(userUuid)));
	}
}
