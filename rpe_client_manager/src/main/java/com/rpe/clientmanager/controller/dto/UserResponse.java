package com.rpe.clientmanager.controller.dto;

import com.rpe.clientmanager.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

/** Never includes the password hash. */
public record UserResponse(UUID id, String username, UserRole role, Instant createdAt) {
}
