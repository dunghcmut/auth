package com.example.auth.dto.response;

public record AuthResponse(
    UserProfileResponse user,
    TokenResponse tokens
) {
}
