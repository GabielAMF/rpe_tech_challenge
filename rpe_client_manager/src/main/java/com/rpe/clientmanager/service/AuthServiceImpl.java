package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.domain.UserRole;
import com.rpe.clientmanager.domain.Username;
import com.rpe.clientmanager.exception.InvalidCredentialsException;
import com.rpe.clientmanager.exception.UsernameAlreadyExistsException;
import com.rpe.clientmanager.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Passwords and tokens are never logged. */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    // Hash of a random value, compared against when the username doesn't exist (see login).
    private final String dummyHash;

    public AuthServiceImpl(AppUserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public IssuedToken login(Username username, String rawPassword) {
        Optional<AppUser> user = userRepository.findByUsername(username.value());
        if (user.isEmpty()) {
            // Still run a hash check so an unknown username takes as long as a wrong password
            // (otherwise response time reveals which usernames exist).
            passwordEncoder.matches(rawPassword, dummyHash);
            log.warn("Login failed for username '{}'", username);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(rawPassword, user.get().getPasswordHash())) {
            log.warn("Login failed for username '{}'", username);
            throw new InvalidCredentialsException();
        }
        log.info("User '{}' logged in", username);
        return tokenService.issue(user.get());
    }

    @Override
    @Transactional
    public AppUser createUser(Username username, String rawPassword, UserRole role) {
        if (userRepository.existsByUsername(username.value())) {
            throw new UsernameAlreadyExistsException(username.value());
        }
        AppUser user = userRepository.saveAndFlush(new AppUser(username, passwordEncoder.encode(rawPassword), role));
        log.info("Created user id={} username='{}' role={}", user.getId(), username, role);
        return user;
    }
}
