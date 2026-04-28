package com.aigo.speech.jobposting.crawler;

import java.util.Set;

import com.aigo.speech.jobposting.config.PlaywrightProperties;
import com.aigo.speech.jobposting.exception.JobPostingCrawlException;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCrawler implements JobPostingCrawler {

	private static final Set<String> BLOCKED_RESOURCE_TYPES = Set.of("image", "media", "font", "stylesheet");
	private static final Set<String> BLOCKED_DOMAINS = Set.of(
		"google-analytics.com", "googletagmanager.com", "doubleclick.net",
		"facebook.net", "amplitude.com", "mixpanel.com",
		"hotjar.com", "clarity.ms", "segment.io", "segment.com"
	);

	protected final BrowserPool browserPool;
	protected final PlaywrightProperties props;

	protected BaseCrawler(BrowserPool browserPool, PlaywrightProperties props) {
		this.browserPool = browserPool;
		this.props = props;
	}

	protected String crawlWithSelectors(String url, String... selectors) {
		BrowserContext context = browserPool.acquireContext();
		try {
			Page page = context.newPage();
			try {
				blockUnnecessaryResources(page);
				navigate(page, url);

				String text = extractBySelectors(page, selectors);
				if (text != null && !text.isBlank()) {
					log.debug("[Crawler] 셀렉터 추출 완료. url={}, length={}", url, text.length());
					return truncate(text);
				}

				// 셀렉터 불일치 시 body 전체 텍스트로 폴백
				log.debug("[Crawler] 셀렉터 불일치, body 텍스트 폴백. url={}", url);
				Object bodyText = page.evaluate("document.body.innerText");
				String body = bodyText != null ? bodyText.toString().strip() : "";
				return truncate(body);

			} catch (TimeoutError e) {
				throw new JobPostingCrawlException("페이지 로드 타임아웃: " + url, e);
			} catch (com.microsoft.playwright.PlaywrightException e) {
				throw new JobPostingCrawlException("페이지 로드 실패: " + url, e);
			} finally {
				closeSilently(page);
			}
		} finally {
			closeSilently(context);
		}
	}

	private void blockUnnecessaryResources(Page page) {
		page.route("**/*", route -> {
			String type = route.request().resourceType();
			String requestUrl = route.request().url();
			if (BLOCKED_RESOURCE_TYPES.contains(type) || isTrackerDomain(requestUrl)) {
				route.abort();
			} else {
				route.resume();
			}
		});
	}

	private boolean isTrackerDomain(String url) {
		return BLOCKED_DOMAINS.stream().anyMatch(url::contains);
	}

	private void navigate(Page page, String url) {
		page.navigate(url, new Page.NavigateOptions()
			.setTimeout(props.navigationTimeoutMs())
			.setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

		// React/SPA 동적 콘텐츠 대기. 타임아웃 시 DOMCONTENTLOADED 기준으로 계속 진행
		try {
			page.waitForLoadState(LoadState.NETWORKIDLE,
				new Page.WaitForLoadStateOptions().setTimeout(5000));
		} catch (TimeoutError e) {
			log.debug("[Crawler] NETWORKIDLE 타임아웃, DOMCONTENTLOADED 기준으로 계속. url={}", url);
		}
	}

	private String extractBySelectors(Page page, String[] selectors) {
		for (String selector : selectors) {
			try {
				page.waitForSelector(selector,
					new Page.WaitForSelectorOptions().setTimeout(3000));
				Object text = page.evaluate(
					"sel => document.querySelector(sel)?.innerText ?? ''", selector
				);
				if (text != null) {
					String extracted = text.toString().strip();
					if (extracted.length() >= 100) {
						return extracted;
					}
				}
			} catch (TimeoutError ignored) {
			} catch (Exception e) {
				log.debug("[Crawler] 셀렉터 시도 실패: selector={}", selector);
			}
		}
		return null;
	}

	private String truncate(String text) {
		if (text == null || text.length() <= props.maxTextLength()) return text;
		return text.substring(0, props.maxTextLength());
	}

	private void closeSilently(AutoCloseable resource) {
		try {
			resource.close();
		} catch (Exception ignored) {
		}
	}
}
