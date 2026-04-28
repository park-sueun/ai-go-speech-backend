package com.aigo.speech.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aigo.speech.interview.exception.InterviewSessionNotFoundException;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.interview.exception.QuestionNotFoundException;
import com.aigo.speech.auth.exception.DuplicateEmailException;
import com.aigo.speech.auth.exception.DuplicateNicknameException;
import com.aigo.speech.auth.exception.InvalidCredentialsException;
import com.aigo.speech.auth.exception.InvalidPasswordException;
import com.aigo.speech.auth.exception.InvalidTokenException;
import com.aigo.speech.auth.exception.PasswordMismatchException;
import com.aigo.speech.auth.exception.SamePasswordException;
import com.aigo.speech.auth.exception.TokenExpiredException;
import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.jobposting.exception.InvalidUrlException;
import com.aigo.speech.jobposting.exception.JobPostingCrawlException;
import com.aigo.speech.jobposting.exception.JobPostingNotFoundException;
import com.aigo.speech.jobposting.exception.JobPostingParseException;
import com.aigo.speech.jobposting.exception.UnsupportedSiteException;
import com.aigo.speech.mail.exception.MailSendException;
import com.aigo.speech.mail.exception.MailVerificationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InterviewSessionNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleInterviewSessionNotFound(InterviewSessionNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(InvalidSessionStatusException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalidSessionStatus(InvalidSessionStatusException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(QuestionNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleQuestionNotFound(QuestionNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(MailVerificationException.class)
	public ResponseEntity<ApiResponse<?>> handleVerificationException (
		MailVerificationException e
	) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleValidationException (
		MethodArgumentNotValidException e
	) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getDefaultMessage())
			.findFirst()
			.orElse("입력값이 올바르지 않습니다.");
		return ResponseEntity.badRequest().body(ApiResponse.fail(message));
	}

	@ExceptionHandler(TokenExpiredException.class)
	public ResponseEntity<ApiResponse<?>> handleExpired (TokenExpiredException e) {
		return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalid (InvalidTokenException e) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(MailSendException.class)
	public ResponseEntity<ApiResponse<?>> handleMailSendException (MailSendException e) {
		return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ApiResponse<?>> handleDuplicateEmail (DuplicateEmailException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(DuplicateNicknameException.class)
	public ResponseEntity<ApiResponse<?>> handleDuplicateNickname (DuplicateNicknameException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleUserNotFound (UserNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalidCredentials (InvalidCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(PasswordMismatchException.class)
	public ResponseEntity<ApiResponse<?>> handlePasswordMismatch (PasswordMismatchException e) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(InvalidPasswordException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalidPassword (InvalidPasswordException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(SamePasswordException.class)
	public ResponseEntity<ApiResponse<?>> handleSamePassword (SamePasswordException e) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<?>> handleIllegalState (IllegalStateException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(InvalidUrlException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalidUrl (InvalidUrlException e) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(UnsupportedSiteException.class)
	public ResponseEntity<ApiResponse<?>> handleUnsupportedSite (UnsupportedSiteException e) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(JobPostingCrawlException.class)
	public ResponseEntity<ApiResponse<?>> handleCrawlException (JobPostingCrawlException e) {
		return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(JobPostingParseException.class)
	public ResponseEntity<ApiResponse<?>> handleParseException (JobPostingParseException e) {
		return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
	}

	@ExceptionHandler(JobPostingNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleJobPostingNotFound (JobPostingNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
	}

}
