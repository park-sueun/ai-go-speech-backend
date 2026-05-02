package com.aigo.speech.jobposting.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aigo.speech.global.sse.SseEmitterService;
import com.aigo.speech.jobposting.crawler.CrawlerFactory;
import com.aigo.speech.jobposting.dto.JobPostingDetailResponse;
import com.aigo.speech.jobposting.dto.JobPostingSummaryResponse;
import com.aigo.speech.jobposting.dto.JobPostingSubmitResponse;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.entity.JobPostingStatus;
import com.aigo.speech.jobposting.exception.InvalidUrlException;
import com.aigo.speech.jobposting.exception.JobPostingNotFoundException;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostingAnalyzeService {

	private final JobPostingRepository jobPostingRepository;
	private final UserRepository userRepository;
	private final CrawlerFactory crawlerFactory;
	private final ApplicationEventPublisher eventPublisher;
	private final SseEmitterService sseEmitterService;

	/**
	 * URL 제출 → 즉시 uuid 반환. 동일 URL 중복 시 기존 결과 반환. 신규 시 PENDING 저장 후 비동기 분석 트리거.
	 */
	@Transactional
	public JobPostingSubmitResponse submit(String url, UUID userUuid) {
		validateUrl(url);
		crawlerFactory.getCrawler(url); // 지원 사이트 검증 (UnsupportedSiteException)

		User user = findUser(userUuid);

		Optional<JobPosting> existing = jobPostingRepository.findByUserAndUrl(user, url);
		if (existing.isPresent()) {
			log.info("[JobPosting] 중복 URL - 기존 결과 반환. uuid={}", existing.get().getUuid());
			return JobPostingSubmitResponse.from(existing.get());
		}

		JobPosting jobPosting = jobPostingRepository.save(JobPosting.pending(user, url));
		log.info("[JobPosting] 신규 등록. uuid={}, url={}", jobPosting.getUuid(), url);

		// 트랜잭션 커밋 후 비동기 분석 실행 (@TransactionalEventListener(AFTER_COMMIT))
		eventPublisher.publishEvent(new JobPostingSubmittedEvent(jobPosting.getId()));

		return JobPostingSubmitResponse.from(jobPosting);
	}

	@Transactional(readOnly = true)
	public SseEmitter subscribeToAnalysis(UUID uuid, UUID userUuid) {
		User user = findUser(userUuid);
		JobPosting jp = jobPostingRepository.findByUuidAndUser(uuid, user)
			.orElseThrow(() -> new JobPostingNotFoundException("채용공고를 찾을 수 없습니다."));

		// race condition: 이미 완료/실패된 경우 즉시 이벤트 전송
		if (jp.getStatus() == JobPostingStatus.DONE) {
			SseEmitter emitter = sseEmitterService.register(uuid);
			sseEmitterService.sendEvent(uuid, "JOB_POSTING_ANALYSIS_DONE",
				Map.of("jobPostingUuid", uuid.toString()));
			sseEmitterService.complete(uuid);
			return emitter;
		}
		if (jp.getStatus() == JobPostingStatus.FAILED) {
			SseEmitter emitter = sseEmitterService.register(uuid);
			sseEmitterService.sendEvent(uuid, "ERROR",
				Map.of("message", "채용공고 분석에 실패했습니다."));
			sseEmitterService.complete(uuid);
			return emitter;
		}

		return sseEmitterService.register(uuid);
	}

	@Transactional(readOnly = true)
	public JobPostingDetailResponse getDetail(UUID uuid, UUID userUuid) {
		User user = findUser(userUuid);
		JobPosting jp = jobPostingRepository.findByUuidAndUser(uuid, user)
			.orElseThrow(() -> new JobPostingNotFoundException("채용공고를 찾을 수 없습니다."));
		return JobPostingDetailResponse.from(jp);
	}

	@Transactional(readOnly = true)
	public List<JobPostingSummaryResponse> getMyList(UUID userUuid) {
		User user = findUser(userUuid);
		return jobPostingRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
			.map(JobPostingSummaryResponse::from)
			.toList();
	}

	@Transactional
	public JobPostingDetailResponse updateCompanyName(UUID uuid, UUID userUuid, String companyName) {
		User user = findUser(userUuid);
		JobPosting jp = jobPostingRepository.findByUuidAndUser(uuid, user)
			.orElseThrow(() -> new JobPostingNotFoundException("채용공고를 찾을 수 없습니다."));
		jp.updateCompanyName(companyName);

		return JobPostingDetailResponse.from(jp);
	}

	@Transactional
	public void delete(UUID uuid, UUID userUuid) {
		User user = findUser(userUuid);
		JobPosting jp = jobPostingRepository.findByUuidAndUser(uuid, user)
			.orElseThrow(() -> new JobPostingNotFoundException("채용공고를 찾을 수 없습니다."));
		jobPostingRepository.delete(jp);
		log.info("[JobPosting] 삭제 완료. uuid={}", uuid);
	}

	private User findUser(UUID userUuid) {
		return userRepository.findByUuid(userUuid)
			.orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + userUuid));
	}

	private void validateUrl(String url) {
		try {
			URI uri = URI.create(url);
			String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
				throw new InvalidUrlException("올바른 URL 형식이 아닙니다: " + url);
			}
		} catch (IllegalArgumentException e) {
			throw new InvalidUrlException("올바른 URL 형식이 아닙니다: " + url);
		}
	}
}
