package com.aigo.speech.jobposting.crawler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import com.aigo.speech.jobposting.config.PlaywrightProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import lombok.extern.slf4j.Slf4j;

/**
 * Playwright는 스레드-안전하지 않으므로 ThreadLocal 패턴으로 스레드별 Browser 인스턴스를 관리.
 * crawlerExecutor 스레드풀의 각 스레드가 첫 요청 시 Chromium 프로세스를 하나씩 생성하고 재사용.
 */
@Slf4j
@Component
public class BrowserPool implements DisposableBean {

	private static final List<String> CHROME_ARGS = List.of(
		"--no-sandbox",
		"--disable-dev-shm-usage",
		"--disable-gpu",
		"--disable-extensions",
		"--disable-background-networking",
		"--disable-default-apps",
		"--disable-sync",
		"--disable-translate",
		"--mute-audio",
		"--no-first-run",
		"--safebrowsing-disable-auto-update",
		"--blink-settings=imagesEnabled=false"
	);

	private final PlaywrightProperties props;
	private final CopyOnWriteArrayList<Playwright> allInstances = new CopyOnWriteArrayList<>();
	private final ThreadLocal<Browser> threadLocalBrowser;

	public BrowserPool(PlaywrightProperties props) {
		this.props = props;
		this.threadLocalBrowser = ThreadLocal.withInitial(this::createBrowser);
	}

	private Browser createBrowser() {
		Playwright pw = Playwright.create();
		allInstances.add(pw);
		Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions()
			.setHeadless(props.headless())
			.setArgs(CHROME_ARGS)
		);
		log.info("[BrowserPool] Chromium 시작 - thread={}", Thread.currentThread().getName());
		return browser;
	}

	public BrowserContext acquireContext() {
		Browser browser = threadLocalBrowser.get();
		BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
			.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
			.setViewportSize(1920, 1080)
		);
		ctx.setDefaultTimeout(props.pageTimeoutMs());
		ctx.setDefaultNavigationTimeout(props.navigationTimeoutMs());
		return ctx;
	}

	@Override
	public void destroy() {
		log.info("[BrowserPool] Playwright 인스턴스 {}개 종료 중...", allInstances.size());
		allInstances.forEach(pw -> {
			try {
				pw.close();
			} catch (Exception e) {
				log.warn("[BrowserPool] Playwright 종료 실패: {}", e.getMessage());
			}
		});
	}
}
