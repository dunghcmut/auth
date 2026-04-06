package com.example.auth.service.impl;

import com.example.auth.dto.request.LoginRequest;
import com.example.auth.dto.request.RefreshTokenRequest;
import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.response.AuthResponse;
import com.example.auth.dto.response.UserProfileResponse;
import com.example.auth.entity.User;
import com.example.auth.entity.UserStatus;
import com.example.auth.exception.ConflictException;
import com.example.auth.exception.InvalidCredentialsException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.mapper.AuthMapper;
import com.example.auth.mapper.UserMapper;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtToken;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.TokenType;
import com.example.auth.service.AuthService;
import com.example.auth.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper,
        AuthMapper authMapper,
        JwtTokenProvider jwtTokenProvider,
        RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered.");
        }

        User user = userMapper.toNewUser(request, normalizedEmail, passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        return issueTokens(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException("Invalid email or password.");
        } catch (DisabledException exception) {
            throw new UnauthorizedException("User account is not active.");
        }

        User user = getActiveUserByEmail(normalizedEmail);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        jwtTokenProvider.validateToken(refreshToken, TokenType.REFRESH);

        String email = normalizeEmail(jwtTokenProvider.extractEmail(refreshToken));
        User user = getActiveUserByEmail(email);
        refreshTokenService.validateRefreshToken(user, refreshToken);

        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = getUserByEmail(email);
        refreshTokenService.revokeRefreshToken(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = getUserByEmail(email);
        return userMapper.toUserProfile(user);
    }

    private AuthResponse issueTokens(User user) {
        JwtToken accessToken = jwtTokenProvider.generateAccessToken(user);
        JwtToken refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenService.storeRefreshToken(user, refreshToken.token());
        return authMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    private User getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private User getActiveUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmailAndStatus(normalizedEmail, UserStatus.ACTIVE)
            .orElseThrow(() -> new UnauthorizedException("User account is not active."));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new UnauthorizedException("Authenticated user is missing.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
