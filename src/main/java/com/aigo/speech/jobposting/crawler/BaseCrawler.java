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

		if (!seleniumRemoteUrl.isBlank()) {
			try {
				return new RemoteWebDriver(new URL(seleniumRemoteUrl), options);
			} catch (Exception e) {
				throw new RuntimeException("Selenium 원격 서버 연결 실패: " + seleniumRemoteUrl, e);
			}
		}

		WebDriverManager.chromedriver().setup();
		return new ChromeDriver(options);
	}

	protected String getBodyText (WebDriver driver) {
		return driver.findElement(By.tagName("body")).getText();
	}

	protected void waitForElement (WebDriver driver, By selector, int seconds) {
		new WebDriverWait(driver, Duration.ofSeconds(seconds))
			.until(ExpectedConditions.presenceOfElementLocated(selector));
	}

}
