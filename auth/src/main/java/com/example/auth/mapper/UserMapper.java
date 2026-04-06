package com.example.auth.mapper;

import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.response.UserProfileResponse;
import com.example.auth.entity.Role;
import com.example.auth.entity.User;
import com.example.auth.entity.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toNewUser(RegisterRequest request, String normalizedEmail, String encodedPassword) {
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(encodedPassword);
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public UserProfileResponse toUserProfile(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
