package com.rpe.clientmanager.config;

import com.rpe.clientmanager.domain.UserRole;
import com.rpe.clientmanager.domain.Username;
import com.rpe.clientmanager.repository.AppUserRepository;
import com.rpe.clientmanager.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first ADMIN account on startup (app.security.bootstrap-user), so there is always someone
 * who can log in and create other users. Does nothing if the user already exists or no username is set.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapUserInitializer implements ApplicationRunner {

    private final SecurityProperties securityProperties;
    private final AppUserRepository userRepository;
    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        SecurityProperties.BootstrapUser bootstrap = securityProperties.bootstrapUser();
        if (bootstrap == null || !StringUtils.hasText(bootstrap.username())) {
            return;
        }
        Username username = new Username(bootstrap.username());
        if (userRepository.existsByUsername(username.value())) {
            return;
        }
        if (!StringUtils.hasText(bootstrap.password())) {
            throw new IllegalStateException("app.security.bootstrap-user.password must be set");
        }
        authService.createUser(username, bootstrap.password(), UserRole.ADMIN);
        log.info("Created bootstrap admin user '{}'", username);
    }
}
