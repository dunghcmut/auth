package com.example.auth.security;

import com.example.auth.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/refresh"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        CustomUserDetailsService userDetailsService,
        JwtAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && PUBLIC_POST_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            jwtTokenProvider.validateToken(token, TokenType.ACCESS);
            String email = jwtTokenProvider.extractEmail(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (InvalidTokenException | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute("jwt_error", exception.getMessage());
            authenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException(exception.getMessage(), exception)
            );
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute("jwt_error", "Failed to authenticate the provided token.");
            authenticationEntryPoint.commence(
                request,
                response,
                new AuthenticationServiceException("JWT authentication failed.", exception)
            );
        }
    }
}
