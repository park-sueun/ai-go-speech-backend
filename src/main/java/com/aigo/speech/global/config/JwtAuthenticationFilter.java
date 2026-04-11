package com.aigo.speech.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String bearerToken = request.getHeader("Authorization");
    if(bearerToken != null && bearerToken.startsWith("Bearer ")) {
      String token = bearerToken.substring(7);

      if(jwtTokenProvider.validateToken(token)) { // 토큰 유효성 검증
        String email = jwtTokenProvider.getEmail(token);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(email, null, null);

        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    filterChain.doFilter(request, response);
  }

}
