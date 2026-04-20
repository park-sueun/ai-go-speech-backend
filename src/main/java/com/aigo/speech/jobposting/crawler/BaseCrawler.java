package com.aigo.speech.jobposting.crawler;

import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;

import com.aigo.speech.jobposting.exception.JobPostingCrawlException;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCrawler implements JobPostingCrawler {

	@Value("${selenium.remote-url:}")
	private String seleniumRemoteUrl;

	protected WebDriver createDriver () {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-gpu");
		options.addArguments(
			"user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
				"AppleWebKit/537.36 (KHTML, like Gecko) " +
				"Chrome/120.0.0.0 Safari/537.36"
		);

		try {
			if (!seleniumRemoteUrl.isBlank()) {
				return new RemoteWebDriver(new URL(seleniumRemoteUrl), options);
			}
			WebDriverManager.chromedriver().setup();
			return new ChromeDriver(options);
		} catch (JobPostingCrawlException e) {
			throw e;
		} catch (Exception e) {
			log.error("[BaseCrawler] WebDriver 생성 실패. remoteUrl={}", seleniumRemoteUrl, e);
			throw new JobPostingCrawlException("크롬 드라이버 초기화 실패");
		}
	}

	protected String getBodyText (WebDriver driver) {
		return driver.findElement(By.tagName("body")).getText();
	}

	protected void waitForElement (WebDriver driver, By selector, int seconds) {
		new WebDriverWait(driver, Duration.ofSeconds(seconds))
			.until(ExpectedConditions.presenceOfElementLocated(selector));
	}

}
