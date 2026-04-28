package com.aigo.speech.jobposting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.aigo.speech.jobposting.config.PlaywrightProperties;
import com.aigo.speech.jobposting.crawler.BrowserPool;
import com.aigo.speech.jobposting.crawler.WantedCrawler;

/**
 * 실행: PLAYWRIGHT_INTEGRATION=true ./gradlew test --tests "*.WantedCrawlerIntegrationTest"
 */
@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_INTEGRATION", matches = "true")
class WantedCrawlerIntegrationTest {

	private static final String TEST_URL = "https://www.wanted.co.kr/wd/353354";

	private BrowserPool browserPool;
	private WantedCrawler wantedCrawler;

	@BeforeEach
	void setUp() {
		PlaywrightProperties props = new PlaywrightProperties(1, 15000, 20000, true, 8000);
		browserPool = new BrowserPool(props);
		wantedCrawler = new WantedCrawler(browserPool, props);
	}

	@AfterEach
	void tearDown() throws Exception {
		browserPool.destroy();
	}

	@Test
	@DisplayName("Wanted 채용공고 스크래핑 - " + TEST_URL)
	void shouldCrawlWantedJobPosting() {
		String result = wantedCrawler.crawl(TEST_URL);

		System.out.println("=== 크롤링 결과 (앞 500자) ===");
		System.out.println(result.substring(0, Math.min(500, result.length())));
		System.out.println("================================");
		System.out.printf("전체 길이: %d자%n", result.length());

		assertThat(result).isNotBlank();
		assertThat(result.length()).isGreaterThan(100);
	}
}
