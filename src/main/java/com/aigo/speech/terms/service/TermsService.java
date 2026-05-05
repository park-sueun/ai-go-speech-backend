package com.aigo.speech.terms.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.terms.dto.TermsRequest;
import com.aigo.speech.terms.dto.TermsResponse;
import com.aigo.speech.terms.entity.Terms;
import com.aigo.speech.terms.repository.TermsRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TermsService {

	private final TermsRepository termsRepository;

	@Transactional
	public UUID createTerms (TermsRequest dto) {
		return termsRepository.save(dto.toEntity()).getUuid();
	}

	public List<TermsResponse> getAllTerms () {
		return termsRepository.findAll().stream()
			.map(TermsResponse::from)
			.collect(Collectors.toList());
	}

	public TermsResponse getTerms (UUID uuid) {
		Terms terms = termsRepository.findByUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("해당 약관이 존재하지 않습니다."));
		return TermsResponse.from(terms);
	}

	@Transactional
	public void updateTerms (UUID uuid, TermsRequest dto) {
		Terms terms = termsRepository.findByUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("해당 약관이 존재하지 않습니다."));
		terms.update(dto.getTitle(), dto.getContent(), dto.getVersion(), dto.getRequired(), dto.getIsActive());
	}

	@Transactional
	public void deleteTerms (UUID uuid) {
		Terms terms = termsRepository.findByUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("해당 약관이 존재하지 않습니다."));
		termsRepository.delete(terms);
	}
}
