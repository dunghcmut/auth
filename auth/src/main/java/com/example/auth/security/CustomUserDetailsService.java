package com.example.auth.security;

import com.example.auth.entity.User;
import com.example.auth.entity.UserStatus;
import com.example.auth.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is not active.");
        }

        return SecurityUserPrincipal.fromUser(user);
    }
}
