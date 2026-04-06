package com.example.auth.security;

import com.example.auth.entity.User;
import com.example.auth.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserPrincipal(
        Long id,
        String email,
        String password,
        UserStatus status,
        Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.status = status;
        this.authorities = authorities;
    }

    public static SecurityUserPrincipal fromUser(User user) {
        return new SecurityUserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getStatus(),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
