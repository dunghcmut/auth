package com.example.auth.service;

import com.example.auth.entity.User;

public interface RefreshTokenService {

    void storeRefreshToken(User user, String refreshToken);

    void validateRefreshToken(User user, String refreshToken);

    void revokeRefreshToken(User user);
}
