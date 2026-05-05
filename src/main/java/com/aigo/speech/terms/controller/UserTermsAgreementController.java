package com.aigo.speech.terms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.terms.dto.UserAgreementResponse;
import com.aigo.speech.terms.service.UserTermsAgreementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-agreements")
@RequiredArgsConstructor
public class UserTermsAgreementController {

	private final UserTermsAgreementService userTermsAgreementService;

	@GetMapping
	public ResponseEntity<List<UserAgreementResponse>> getAgreements (
		@AuthenticationPrincipal String userUuid
	) {
		return ResponseEntity.ok(userTermsAgreementService.getUserAgreements(userUuid));
	}
}
