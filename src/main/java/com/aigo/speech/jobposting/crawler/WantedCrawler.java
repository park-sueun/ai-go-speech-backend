package com.aigo.speech.jobposting.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.exception.JobPostingCrawlException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WantedCrawler extends BaseCrawler {

	private static final String DOMAIN = "wanted.co.kr";

	// 우선순위 순으로 시도할 셀렉터 목록
	private static final By[] CONTENT_SELECTORS = {
		By.cssSelector("[class*='JobDescription_JobDescription']"),
		By.cssSelector("[class*='JobContent']"),
		By.cssSelector("section.job-detail"),
		By.tagName("article"),
	};

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

			// 우선순위 셀렉터 순서대로 시도
			for (By selector : CONTENT_SELECTORS) {
				try {
					waitForElement(driver, selector, 15);
					String text = driver.findElement(selector).getText();
					log.debug("[Wanted] 크롤링 완료. selector={}, length={}", selector, text.length());
					return text;
				} catch (TimeoutException e) {
					log.debug("[Wanted] 셀렉터 매칭 실패, 다음 시도. selector={}", selector);
				}
			}

			// 모든 셀렉터 실패 시 body 전체 텍스트 반환
			log.warn("[Wanted] 모든 셀렉터 실패, body 전체 텍스트 사용. url={}", url);
			String bodyText = getBodyText(driver);
			log.debug("[Wanted] body 텍스트 길이={}", bodyText.length());
			return bodyText;

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