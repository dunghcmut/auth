package com.example.auth.service.impl;

import com.example.auth.entity.User;
import com.example.auth.exception.InvalidTokenException;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.TokenHashProvider;
import com.example.auth.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenHashProvider tokenHashProvider;

    public RefreshTokenServiceImpl(UserRepository userRepository, TokenHashProvider tokenHashProvider) {
        this.userRepository = userRepository;
        this.tokenHashProvider = tokenHashProvider;
    }

    @Override
    @Transactional
    public void storeRefreshToken(User user, String refreshToken) {
        user.updateRefreshTokenHash(tokenHashProvider.hash(refreshToken));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRefreshToken(User user, String refreshToken) {
        String refreshTokenHash = user.getRefreshTokenHash();
        if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
            throw new InvalidTokenException("Refresh token has been revoked.");
        }

        if (!tokenHashProvider.matches(refreshToken, refreshTokenHash)) {
            throw new InvalidTokenException("Refresh token does not match the latest issued token.");
        }
    }

    @Override
    @Transactional
    public void revokeRefreshToken(User user) {
        user.clearRefreshTokenHash();
        userRepository.save(user);
    }
}
