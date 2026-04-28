package com.aigo.speech.jobposting.crawler;

import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.config.PlaywrightProperties;

@Component
public class SaraminCrawler extends BaseCrawler {

	private static final String[] SELECTORS = {
		".job_detail_info",
		".job_detail",
		".recruit_detail",
		".jd-detail-inner",
		".job-description-content",
		"article",
		"main"
	};

	public SaraminCrawler(BrowserPool browserPool, PlaywrightProperties props) {
		super(browserPool, props);
	}

	@Override
	public boolean supports(String url) {
		return url.contains("saramin.co.kr");
	}

	@Override
	public String crawl(String url) {
		return crawlWithSelectors(url, SELECTORS);
	}
}
