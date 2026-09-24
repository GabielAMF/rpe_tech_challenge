package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.controller.dto.UserResponse;
import com.rpe.clientmanager.domain.AppUser;
import org.springframework.stereotype.Component;

/** Converts users into the public API representation (never the password hash). */
@Component
public class UserMapper {

    public UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
