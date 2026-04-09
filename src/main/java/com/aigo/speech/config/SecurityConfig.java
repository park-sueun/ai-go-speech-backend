package com.aigo.speech.config;

import com.aigo.speech.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (개발 단계)
                .csrf(csrf -> csrf.disable())

                // 2. H2 콘솔 쓸 경우 대비
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // 3. 요청 허용 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 4. OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/login/success", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )

                // 5. 기본 로그인 폼 제거
                .formLogin(form -> form.disable());

        return http.build();
    }
}
