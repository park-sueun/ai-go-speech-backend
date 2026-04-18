package com.aigo.speech.jobposting.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.exception.JobPostingCrawlException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WantedCrawler extends BaseCrawler {

	private static final String DOMAIN = "wanted.co.kr";

	// 빌드 해시가 바뀌어도 클래스명 앞부분(JobDescription_JobDescription)으로 매칭
	private static final By CONTENT_SELECTOR =
		By.cssSelector("[class*='JobDescription_JobDescription']");

	@Override
	public boolean supports (String url) {
		return url.contains(DOMAIN);
	}

	@Override
	public String crawl (String url) {
		WebDriver driver = createDriver();
		try {
			log.debug("[Wanted] 크롤링 시작. url={}", url);
			driver.get(url);

			String title = driver.getTitle();
			log.debug("[Wanted] 페이지 타이틀: {}", title);

			// 봇 차단 또는 로그인 요구 감지
			if (title == null || title.isBlank() || title.contains("로그인") || title.contains("Login")) {
				log.warn("[Wanted] 접근 차단 또는 로그인 요구 감지. title={}", title);
				throw new JobPostingCrawlException("원티드 페이지에 접근할 수 없습니다. 로그인이 필요하거나 접근이 차단되었습니다.");
			}

			waitForElement(driver, CONTENT_SELECTOR, 15);
			String text = driver.findElement(CONTENT_SELECTOR).getText();
			log.debug("[Wanted] 크롤링 완료. length={}", text.length());
			return text;
		} catch (JobPostingCrawlException e) {
			throw e;
		} catch (Exception e) {
			log.error("[Wanted] 크롤링 실패. url={}", url, e);
			throw new JobPostingCrawlException("원티드 페이지 수집 실패");
		} finally {
			driver.quit();
		}
	}
}