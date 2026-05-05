package com.aigo.speech.terms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@GetMapping("/{userId}")
	public ResponseEntity<List<UserAgreementResponse>> getAgreements (
		@PathVariable Long userId
	) {
		return ResponseEntity.ok(userTermsAgreementService.getUserAgreements(userId));
	}

}
