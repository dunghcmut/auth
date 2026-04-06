package com.example.auth.security;

import com.example.auth.config.JwtProperties;
import com.example.auth.entity.User;
import com.example.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties, JwtSigningKeyFactory jwtSigningKeyFactory) {
        this.jwtProperties = jwtProperties;
        this.signingKey = jwtSigningKeyFactory.createSigningKey();
    }

    public JwtToken generateAccessToken(User user) {
        return generateToken(
            user.getEmail(),
            TokenType.ACCESS,
            jwtProperties.accessTokenExpiration(),
            Map.of(ROLE_CLAIM, user.getRole().name())
        );
    }

    public JwtToken generateRefreshToken(User user) {
        return generateToken(
            user.getEmail(),
            TokenType.REFRESH,
            jwtProperties.refreshTokenExpiration(),
            Map.of()
        );
    }

    public void validateToken(String token, TokenType expectedType) {
        Claims claims = parseClaims(token);
        String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.name().equals(actualType)) {
            throw new InvalidTokenException("Token type is invalid.");
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    private JwtToken generateToken(String subject, TokenType tokenType, Duration ttl, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        String token = Jwts.builder()
            .issuer(jwtProperties.issuer())
            .subject(subject)
            .claims(extraClaims)
            .claim(TOKEN_TYPE_CLAIM, tokenType.name())
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();

        return new JwtToken(token, expiresAt);
    }

    private Claims parseClaims(String token) {
        try {
            return parseToken(token).getPayload();
        } catch (ExpiredJwtException exception) {
            throw new InvalidTokenException("Token has expired.");
        } catch (UnsupportedJwtException | MalformedJwtException | SecurityException exception) {
            throw new InvalidTokenException("Token is invalid.");
        } catch (IllegalArgumentException exception) {
            throw new InvalidTokenException("Token value is missing.");
        }
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token);
    }
}
