package com.aigo.speech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SpeechApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpeechApplication.class, args);
	}

}
