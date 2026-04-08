package com.aigo.speech.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupLogger implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        String profiles = activeProfiles.length > 0
                ? String.join(", ", activeProfiles)
                : "default";

        log.info("------------------------------------------");
        log.info("  >>> Active Profile : {}  <<<", profiles);
        log.info("------------------------------------------");
    }
}
