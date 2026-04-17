package com.aigo.speech.auth.jwt;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider (
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiration-min}") long accessTokenMin,
        @Value("${jwt.refresh-expiration-days}") long refreshTokenDays
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenMin * 60 * 1000L;
        this.refreshTokenExpiration = refreshTokenDays * 24 * 60 * 60 * 1000L;
    }

    public String createAccessToken (UUID uuid) {
        return createToken(uuid.toString(), accessTokenExpiration);
    }

    public String createRefreshToken (UUID uuid) {
        return createToken(uuid.toString(), refreshTokenExpiration);
    }

    public String createToken (String subject, long expire) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + expire))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken (String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getUuid (String token) {
        String subject = Jwts.parserBuilder().setSigningKey(key).build()
            .parseClaimsJws(token).getBody().getSubject();
        return UUID.fromString(subject);
    }

    public boolean isTokenExpired (String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
