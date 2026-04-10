package com.aigo.speech.auth.dto;

public record EmailVerificationConfirmRequest(
        String email,
        String code
) {
}
