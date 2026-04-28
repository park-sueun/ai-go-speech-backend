package com.aigo.speech.jobposting.crawler;

import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.config.PlaywrightProperties;

@Component
public class JobKoreaCrawler extends BaseCrawler {

	private static final String[] SELECTORS = {
		".cont-duty",
		".duty-area",
		".recruit-detail",
		".tb-post",
		".recruit_info",
		"article",
		"main"
	};

	public JobKoreaCrawler(BrowserPool browserPool, PlaywrightProperties props) {
		super(browserPool, props);
	}

	@Override
	public boolean supports(String url) {
		return url.contains("jobkorea.co.kr");
	}

	@Override
	public String crawl(String url) {
		return crawlWithSelectors(url, SELECTORS);
	}
}
