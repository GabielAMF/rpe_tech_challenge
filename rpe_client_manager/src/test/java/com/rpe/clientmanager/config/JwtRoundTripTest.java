package com.rpe.clientmanager.config;

import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.domain.UserRole;
import com.rpe.clientmanager.domain.Username;
import com.rpe.clientmanager.service.IssuedToken;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Issues tokens with the real encoder and reads them back with the real decoder from SecurityConfig. */
class JwtRoundTripTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";
    private static final UUID USER_ID = UUID.randomUUID();

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void decoderAcceptsIssuedTokenWithExpectedClaims() {
        SecurityProperties properties = properties(SECRET, "rpe-client-manager");
        IssuedToken token = tokenService(properties, Clock.systemUTC()).issue(admin());

        Jwt jwt = config.jwtDecoder(properties).decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void decoderRejectsExpiredToken() {
        SecurityProperties properties = properties(SECRET, "rpe-client-manager");
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        IssuedToken token = tokenService(properties, twoHoursAgo).issue(admin());

        assertThatThrownBy(() -> config.jwtDecoder(properties).decode(token.value()))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void decoderRejectsTokenSignedWithAnotherSecret() {
        IssuedToken token = tokenService(properties("another-secret-that-is-also-32-bytes-long", "rpe-client-manager"),
                Clock.systemUTC()).issue(admin());

        assertThatThrownBy(() -> config.jwtDecoder(properties(SECRET, "rpe-client-manager")).decode(token.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void decoderRejectsTokenFromAnotherIssuer() {
        IssuedToken token = tokenService(properties(SECRET, "someone-else"), Clock.systemUTC()).issue(admin());

        assertThatThrownBy(() -> config.jwtDecoder(properties(SECRET, "rpe-client-manager")).decode(token.value()))
                .isInstanceOf(JwtException.class);
    }

    private JwtTokenService tokenService(SecurityProperties properties, Clock clock) {
        return new JwtTokenService(config.jwtEncoder(properties), properties, clock);
    }

    private static SecurityProperties properties(String secret, String issuer) {
        return new SecurityProperties(new SecurityProperties.Jwt(secret, Duration.ofHours(1), issuer), null);
    }

    private static AppUser admin() {
        AppUser user = new AppUser(new Username("admin"), "hash", UserRole.ADMIN);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
