package com.aigo.speech.jobposting.crawler;

public interface JobPostingCrawler {
	boolean supports (String url);

	String crawl (String url);
}
