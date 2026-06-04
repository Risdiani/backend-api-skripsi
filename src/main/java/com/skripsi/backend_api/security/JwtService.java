package com.skripsi.backend_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey signingKey() {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        log.info("Generating JWT token untuk username: {}", username);
        Map<String, Object> claims = new HashMap<>();
        String token = createToken(claims, username);
        log.debug("JWT token berhasil generated");
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
        log.debug("Token created dengan expiration: {} ms", expirationMs);
        return token;
    }

    public String extractUsername(String token) {
        log.debug("Extracting username dari token");
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        log.debug("Extracting JTI dari token");
        return extractAllClaims(token).getId();
    }

    public Date extractExpiration(String token) {
        log.debug("Extracting expiration dari token");
        return extractAllClaims(token).getExpiration();
    }

    public boolean validateToken(String token, String expectedUsername) {
        try {
            final String extractedUsername = extractUsername(token);
            boolean isValid = extractedUsername != null
                    && extractedUsername.equals(expectedUsername)
                    && !isTokenExpired(token);
            
            if (isValid) {
                log.debug("Token valid untuk username: {}", expectedUsername);
            } else {
                log.warn("Token tidak valid untuk username: {}", expectedUsername);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Error saat validasi token", e);
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}