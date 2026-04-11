package com.aigo.speech.global.config;

import com.aigo.speech.auth.jwt.JwtAuthenticationFilter;
import com.aigo.speech.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .requestMatchers("/api/auth/**", // jwt
                "/api/user/**",
                "/v3/api-docs/**", // swagger 설정
                "/swagger-ui/**",
                "/swagger-ui.html").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
