package com.aigo.speech.global.config;

import com.aigo.speech.auth.jwt.JwtAuthenticationFilter;
import com.aigo.speech.auth.jwt.JwtTokenProvider;
import com.aigo.speech.auth.oauth2.OAuth2SuccessHandler;
import com.aigo.speech.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;

  @Bean
  public BCryptPasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // jwt 토큰 사용
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/auth/**",
                "/oauth2/**",
                "/login/**",
                "/api/user/**",
                "/v3/api-docs/**", // swagger 설정
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll()
            .requestMatchers(HttpMethod.PATCH, "/api/auth/password").authenticated()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2SuccessHandler)
        )
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
