package com.aigo.speech.jobposting.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aigo.speech.global.dto.ApiResponse;
import com.aigo.speech.jobposting.dto.JobPostingDetailResponse;
import com.aigo.speech.jobposting.dto.JobPostingSummaryResponse;
import com.aigo.speech.jobposting.dto.JobPostingSubmitRequest;
import com.aigo.speech.jobposting.dto.JobPostingSubmitResponse;
import com.aigo.speech.jobposting.dto.JobPostingUpdateRequest;
import com.aigo.speech.jobposting.service.JobPostingAnalyzeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

	private final JobPostingAnalyzeService analyzeService;

	/** URL 제출 → uuid 즉시 반환 (PENDING 상태). 동일 URL은 기존 결과 반환. */
	@PostMapping
	public ResponseEntity<ApiResponse<JobPostingSubmitResponse>> submit(
		@RequestBody @Valid JobPostingSubmitRequest request,
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		JobPostingSubmitResponse response = analyzeService.submit(request.getUrl(), userUuid);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
	}

	/** 내 채용공고 목록 조회 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<JobPostingSummaryResponse>>> getMyList(
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(analyzeService.getMyList(userUuid)));
	}

	/** 채용공고 분석 완료 SSE 구독. 이미 완료/실패 시 즉시 이벤트 반환 */
	@GetMapping(value = "/{uuid}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(
		@PathVariable UUID uuid,
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return analyzeService.subscribeToAnalysis(uuid, userUuid);
	}

	/** 채용공고 상세 조회 (status가 DONE이면 분석 결과 포함) */
	@GetMapping("/{uuid}")
	public ResponseEntity<ApiResponse<JobPostingDetailResponse>> getDetail(
		@PathVariable UUID uuid,
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(analyzeService.getDetail(uuid, userUuid)));
	}

	/** 회사명 수정 */
	@PatchMapping("/{uuid}")
	public ResponseEntity<ApiResponse<JobPostingDetailResponse>> update(
		@PathVariable UUID uuid,
		@RequestBody @Valid JobPostingUpdateRequest request,
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		JobPostingDetailResponse jp = analyzeService.updateCompanyName(uuid, userUuid, request.getCompanyName());
		return ResponseEntity.ok(ApiResponse.success(jp));
	}

	/** 채용공고 삭제 */
	@DeleteMapping("/{uuid}")
	public ResponseEntity<ApiResponse<Void>> delete(
		@PathVariable UUID uuid,
		Authentication authentication
	) {
		UUID userUuid = UUID.fromString(authentication.getName());
		analyzeService.delete(uuid, userUuid);
		return ResponseEntity.ok(ApiResponse.success(null));
	}
}
