package com.example.auth.service;

import com.example.auth.dto.request.LoginRequest;
import com.example.auth.dto.request.RefreshTokenRequest;
import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.response.AuthResponse;
import com.example.auth.dto.response.UserProfileResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String email);

    UserProfileResponse getCurrentUserProfile(String email);
}
