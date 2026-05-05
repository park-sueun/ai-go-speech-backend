package com.aigo.speech.terms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.terms.dto.TermsRequest;
import com.aigo.speech.terms.dto.TermsResponse;
import com.aigo.speech.terms.service.TermsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

	private final TermsService termsService;

	@PostMapping
	public ResponseEntity<UUID> create (@RequestBody TermsRequest dto) {
		return ResponseEntity.ok(termsService.createTerms(dto));
	}

	@GetMapping
	public ResponseEntity<List<TermsResponse>> list (
		@RequestParam(required = false) Boolean isActive
	) {
		return ResponseEntity.ok(termsService.getAllTerms(isActive));
	}

	@GetMapping("/{uuid}")
	public ResponseEntity<TermsResponse> detail (@PathVariable UUID uuid) {
		return ResponseEntity.ok(termsService.getTerms(uuid));
	}

	@PutMapping("/{uuid}")
	public ResponseEntity<Void> update (@PathVariable UUID uuid, @RequestBody TermsRequest dto) {
		termsService.updateTerms(uuid, dto);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{uuid}")
	public ResponseEntity<Void> delete (@PathVariable UUID uuid) {
		termsService.deleteTerms(uuid);
		return ResponseEntity.noContent().build();
	}
}
