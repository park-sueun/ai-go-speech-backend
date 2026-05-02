package com.aigo.speech.terms.dto;

import java.time.LocalDateTime;

import com.aigo.speech.terms.entity.UserTermsAgreement;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAgreementResponse {
	private Long agreementId;
	private Long termsId;
	private String termsTitle;
	private String termsVersion;
	private LocalDateTime agreementAt;

	public static UserAgreementResponse from (UserTermsAgreement userTermsAgreement) {
		return UserAgreementResponse.builder()
			.agreementId(userTermsAgreement.getId())
			.termsId(userTermsAgreement.getTerms().getId())
			.termsTitle(userTermsAgreement.getTerms().getTitle())
			.agreementAt(userTermsAgreement.getAgreedAt())
			.build();
	}
}
