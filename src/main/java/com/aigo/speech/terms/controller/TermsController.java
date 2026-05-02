package com.aigo.speech.terms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigo.speech.terms.dto.TermsRequest;
import com.aigo.speech.terms.dto.TermsResponse;
import com.aigo.speech.terms.service.TermsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {
	private final TermsService termsService;

	@PostMapping
	public ResponseEntity<Long> create (@RequestBody TermsRequest dto) {
		return ResponseEntity.ok(termsService.createTerms(dto));
	}

	@GetMapping
	public ResponseEntity<List<TermsResponse>> list () {
		return ResponseEntity.ok(termsService.getAllTerms());
	}

	@GetMapping("/{id}")
	public ResponseEntity<TermsResponse> detail (@PathVariable Long id) {
		return ResponseEntity.ok(termsService.getTerms(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Void> update (@PathVariable Long id, @RequestBody TermsRequest dto) {
		termsService.updateTerms(id, dto);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete (@PathVariable Long id) {
		termsService.deleteTerms(id);
		return ResponseEntity.noContent().build();
	}
}
