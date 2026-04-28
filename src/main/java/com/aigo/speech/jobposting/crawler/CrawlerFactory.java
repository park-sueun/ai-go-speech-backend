package com.aigo.speech.jobposting.crawler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.exception.UnsupportedSiteException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CrawlerFactory {

	private final List<JobPostingCrawler> crawlers;

	public JobPostingCrawler getCrawler(String url) {
		return crawlers.stream()
			.filter(c -> c.supports(url))
			.findFirst()
			.orElseThrow(() -> new UnsupportedSiteException("지원하지 않는 채용 사이트입니다: " + url));
	}
}
