package com.example.auth.security;

import java.time.Instant;

public record JwtToken(
    String token,
    Instant expiresAt
) {
}
