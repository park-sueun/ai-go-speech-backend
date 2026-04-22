package com.aigo.speech.jobposting.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.exception.JobPostingCrawlException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JobKoreaCrawler extends BaseCrawler {

	private static final String DOMAIN = "jobkorea.co.kr";
	private static final By[] CONTENT_SELECTORS = {
		By.cssSelector(".cont-duty"),
		By.cssSelector(".duty-area"),
		By.cssSelector(".recruit-detail"),
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
			log.debug("[JobKorea] 크롤링 시작. url={}", url);
			driver.get(url);

			for (By selector : CONTENT_SELECTORS) {
				try {
					waitForElement(driver, selector, 15);
					String text = driver.findElement(selector).getText();
					log.debug("[JobKorea] 크롤링 완료. selector={}, length={}", selector, text.length());
					return text;
				} catch (TimeoutException e) {
					log.debug("[JobKorea] 셀렉터 매칭 실패, 다음 시도. selector={}", selector);
				}
			}

			log.warn("[JobKorea] 모든 셀렉터 실패, body 전체 텍스트 사용. url={}", url);
			String bodyText = getBodyText(driver);
			log.debug("[JobKorea] body 텍스트 길이={}", bodyText.length());
			return bodyText;

		} catch (JobPostingCrawlException e) {
			throw e;
		} catch (Exception e) {
			log.error("[JobKorea] 크롤링 실패. url={}", url, e);
			throw new JobPostingCrawlException("잡코리아 페이지 수집 실패");
		} finally {
			driver.quit();
		}
	}
}
