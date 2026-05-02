package com.aigo.speech.terms.dto;

import com.aigo.speech.terms.entity.Terms;
import com.aigo.speech.terms.entity.TermsType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsRequest {
	private TermsType type;
	private String title;
	private String content;
	private String version;
	private Boolean required;
	private Boolean isActive;

	public Terms toEntity () {
		return Terms.builder()
			.type(this.type)
			.title(this.title)
			.content(this.content)
			.version(this.version)
			.required(this.required != null ? this.required : true)
			.isActive(this.isActive != null ? this.isActive : true)
			.build();
	}
}
