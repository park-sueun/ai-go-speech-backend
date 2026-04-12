package com.aigo.speech.auth.jwt;

import com.aigo.speech.auth.exception.InvalidTokenException;
import com.aigo.speech.auth.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class PasswordResetTokenProvider {

    @Value("${jwt.reset-secret}")
    private String resetSecret;

    @Value("${jwt.reset-expiration-min}")
    private long resetExpirationMin;

    private static final String CLAIM_TYPE = "password-reset";

    private SecretKey getSigninKey() {
        return Keys.hmacShaKeyFor(resetSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", CLAIM_TYPE)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(resetExpirationMin * 60)))
                .signWith(getSigninKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateAndGetEmail(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigninKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!CLAIM_TYPE.equals(claims.get("type"))) {
                throw new InvalidTokenException("유효하지 않은 토큰 타입입니다.");
            }

            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("만료된 토큰입니다.");
        } catch (JwtException e) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }
    }
}
