package com.example.auth.dto.response;

import java.time.Instant;

public record TokenResponse(
    String tokenType,
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
}
