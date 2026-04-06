package com.example.auth.mapper;

import com.example.auth.dto.response.AuthResponse;
import com.example.auth.dto.response.TokenResponse;
import com.example.auth.entity.User;
import com.example.auth.security.JwtToken;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserMapper userMapper;

    public AuthMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AuthResponse toAuthResponse(User user, JwtToken accessToken, JwtToken refreshToken) {
        return new AuthResponse(
            userMapper.toUserProfile(user),
            new TokenResponse(
                TOKEN_TYPE,
                accessToken.token(),
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.expiresAt()
            )
        );
    }
}
