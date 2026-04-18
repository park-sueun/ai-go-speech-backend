package com.aigo.speech.jobposting.service;

import java.net.URL;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.aigo.speech.jobposting.crawler.CrawlerFactory;
import com.aigo.speech.jobposting.crawler.JobPostingCrawler;
import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;
import com.aigo.speech.jobposting.exception.InvalidUrlException;
import com.aigo.speech.jobposting.exception.JobPostingCrawlException;
import com.aigo.speech.jobposting.parser.JobPostingParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostingAnalyzeService {

	private final CrawlerFactory crawlerFactory;
	private final JobPostingParser jobPostingParser;

	public JobPostingAnalyzeResponse analyze (String url) {
		log.info("[JobPosting] 분석 시작. url={}", url);

		validateUrl(url);

		JobPostingCrawler crawler = crawlerFactory.getCrawler(url);
		String rawText = crawler.crawl(url);

		if (!StringUtils.hasText(rawText)) {
			throw new JobPostingCrawlException("수집된 텍스트가 없습니다.");
		}

		log.info("[JobPosting] 크롤링 완료. textLength={}", rawText.length());

		JobPostingAnalyzeResponse result = jobPostingParser.parse(rawText);

		log.info(
			"[JobPosting] 파싱 완료. company={}, position={}",
			result.companyName(), result.position()
		);

		return result;
	}

	private void validateUrl (String url) {
		try {
			new URL(url).toURI();
		} catch (Exception e) {
			throw new InvalidUrlException("유효하지 않은 URL 형식입니다: " + url);
		}
	}
}
