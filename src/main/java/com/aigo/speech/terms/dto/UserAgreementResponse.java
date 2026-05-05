package com.aigo.speech.terms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.aigo.speech.terms.entity.UserTermsAgreement;

public record UserAgreementResponse(
	UUID uuid,
	TermsInfo terms,
	LocalDateTime agreedAt
) {

	public record TermsInfo(UUID uuid, String title, String version) {}

	public static UserAgreementResponse from(UserTermsAgreement agreement) {
		return new UserAgreementResponse(
			agreement.getUuid(),
			new TermsInfo(
				agreement.getTerms().getUuid(),
				agreement.getTerms().getTitle(),
				agreement.getTerms().getVersion()
			),
			agreement.getAgreedAt()
		);
	}
}
