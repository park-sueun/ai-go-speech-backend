package com.aigo.speech.jobposting.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.aigo.speech.jobposting.crawler.CrawlerFactory;
import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.exception.JobPostingCrawlException;
import com.aigo.speech.jobposting.parser.JobPostingParser;
import com.aigo.speech.jobposting.repository.JobPostingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobPostingAnalysisProcessor {

	private final JobPostingRepository jobPostingRepository;
	private final CrawlerFactory crawlerFactory;
	private final JobPostingParser jobPostingParser;

	/**
	 * submit() 트랜잭션이 커밋된 후 실행 → entity가 DB에 확정된 상태에서 크롤링 시작.
	 * crawlerExecutor 스레드풀에서 비동기 실행.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("crawlerExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(JobPostingSubmittedEvent event) {
		JobPosting jobPosting = jobPostingRepository.findById(event.jobPostingId())
			.orElseThrow(() -> new IllegalStateException("JobPosting not found: " + event.jobPostingId()));

		log.info("[Processor] 분석 시작. uuid={}, url={}", jobPosting.getUuid(), jobPosting.getUrl());

		try {
			jobPosting.markAnalyzing();
			jobPostingRepository.save(jobPosting);

			String rawText = crawlerFactory.getCrawler(jobPosting.getUrl()).crawl(jobPosting.getUrl());
			if (rawText == null || rawText.length() < 50) {
				throw new JobPostingCrawlException("채용공고 내용을 추출하지 못했습니다: " + jobPosting.getUrl());
			}

			JobPostingAnalyzeResponse result = jobPostingParser.parse(rawText);
			jobPosting.markDone(result);
			jobPostingRepository.save(jobPosting);

			log.info("[Processor] 분석 완료. uuid={}", jobPosting.getUuid());

		} catch (Exception e) {
			log.error("[Processor] 분석 실패. uuid={}, reason={}", jobPosting.getUuid(), e.getMessage());
			jobPosting.markFailed(e.getMessage());
			jobPostingRepository.save(jobPosting);
		}
	}
}
