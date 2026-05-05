package com.aigo.speech.terms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.aigo.speech.terms.entity.Terms;
import com.aigo.speech.terms.entity.TermsType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsResponse {
	private UUID uuid;
	private TermsType type;
	private String title;
	private String content;
	private String version;
	private Boolean required;
	private Boolean isActive;
	private LocalDateTime createdAt;

	public static TermsResponse from (Terms terms) {
		return TermsResponse.builder()
			.uuid(terms.getUuid())
			.type(terms.getType())
			.title(terms.getTitle())
			.content(terms.getContent())
			.version(terms.getVersion())
			.required(terms.getRequired())
			.isActive(terms.getIsActive())
			.createdAt(terms.getCreatedAt())
			.build();
	}
}
