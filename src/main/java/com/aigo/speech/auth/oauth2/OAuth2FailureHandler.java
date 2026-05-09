package com.aigo.speech.auth.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

	@Value("${app.base-url}")
	private String baseUrl;

	@Override
	public void onAuthenticationFailure (
		HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception
	) throws IOException {
		String errorCode = "oauth2_error";
		if (exception instanceof OAuth2AuthenticationException oae) {
			errorCode = oae.getError().getErrorCode();
		}
		response.sendRedirect(baseUrl + "/oauth2/callback?error=" + errorCode);
	}
}
