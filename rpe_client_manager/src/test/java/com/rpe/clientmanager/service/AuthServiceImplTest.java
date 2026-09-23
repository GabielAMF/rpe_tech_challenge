package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.domain.UserRole;
import com.rpe.clientmanager.domain.Username;
import com.rpe.clientmanager.exception.InvalidCredentialsException;
import com.rpe.clientmanager.exception.UsernameAlreadyExistsException;
import com.rpe.clientmanager.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Username ADMIN = new Username("admin");

    // Low cost factor keeps the test fast; the real encoder uses the default.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private TokenService tokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, tokenService);
    }

    @Test
    void loginReturnsTokenForCorrectPassword() {
        AppUser user = new AppUser(ADMIN, passwordEncoder.encode("secret-pass"), UserRole.ADMIN);
        IssuedToken token = new IssuedToken("jwt", Instant.now());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(token);

        assertThat(authService.login(ADMIN, "secret-pass")).isEqualTo(token);
    }

    @Test
    void loginRejectsWrongPassword() {
        AppUser user = new AppUser(ADMIN, passwordEncoder.encode("secret-pass"), UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(ADMIN, "wrong-pass"))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginRejectsUnknownUserWithSameException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(ADMIN, "secret-pass"))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(tokenService);
    }

    @Test
    void createUserStoresHashNotRawPassword() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser user = authService.createUser(new Username(" Bob "), "secret-pass", UserRole.USER);

        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPasswordHash()).isNotEqualTo("secret-pass");
        assertThat(passwordEncoder.matches("secret-pass", user.getPasswordHash())).isTrue();
    }

    @Test
    void createUserRejectsTakenUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser(ADMIN, "secret-pass", UserRole.USER))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }
}
