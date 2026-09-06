package com.edafa.sms_submitter.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final String SECRET_KEY_STRING = "your-very-secure-and-long-secret-key-that-is-at-least-256-bits!";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME_MS = 12 * 60 * 60 * 1000; // 12 hrs

    public String generateToken(UserDetails userDetails) {
        log.info("Generating JWT token for user: {}", userDetails.getUsername());
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isUsernameMatch = (username != null && username.equals(userDetails.getUsername()));
        boolean isNotExpired = !isTokenExpired(token);
        boolean isEnabled = userDetails.isEnabled();

        boolean isValid = isUsernameMatch && isNotExpired && isEnabled;

        if (!isValid) {
            log.warn("JWT token validation failed for user: {}. MatchesUsername={}, NotExpired={}, Enabled={}",
                    userDetails.getUsername(), isUsernameMatch, isNotExpired, isEnabled);
        } else {
            log.debug("JWT token successfully validated for user: {}", userDetails.getUsername());
        }
        return isValid;
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        boolean expired = expiration == null || expiration.before(new Date());
        if (expired) {
            log.warn("JWT token has expired. Expiration date: {}", expiration);
        }
        return expired;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claimsResolver.apply(claims);
        } catch (JwtException e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.error("JWT token claim string is empty or null: {}", e.getMessage());
            return null;
        }
    }
}