package com.aigo.speech.jobposting.crawler;

import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.config.PlaywrightProperties;

@Component
public class WantedCrawler extends BaseCrawler {

	private static final String[] SELECTORS = {
		"[class*='JobDescription_JobDescription']",
		"[class*='JobContent']",
		"section[class*='job-detail']",
		"article",
		"main"
	};

	public WantedCrawler(BrowserPool browserPool, PlaywrightProperties props) {
		super(browserPool, props);
	}

	@Override
	public boolean supports(String url) {
		return url.contains("wanted.co.kr");
	}

	@Override
	public String crawl(String url) {
		return crawlWithSelectors(url, SELECTORS);
	}
}
