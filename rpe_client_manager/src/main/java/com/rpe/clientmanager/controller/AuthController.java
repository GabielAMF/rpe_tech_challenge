package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.controller.dto.CreateUserRequest;
import com.rpe.clientmanager.controller.dto.LoginRequest;
import com.rpe.clientmanager.controller.dto.TokenResponse;
import com.rpe.clientmanager.controller.dto.UserResponse;
import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.domain.Username;
import com.rpe.clientmanager.service.AuthService;
import com.rpe.clientmanager.service.IssuedToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final Clock clock;

    /** Public. Exchanges username + password for a bearer token. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        IssuedToken token = authService.login(new Username(request.username()), request.password());
        long expiresIn = Duration.between(clock.instant(), token.expiresAt()).toSeconds();
        return new TokenResponse(token.value(), "Bearer", expiresIn);
    }

    /** ADMIN only (enforced in SecurityConfig). */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        AppUser user = authService.createUser(new Username(request.username()), request.password(), request.role());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
