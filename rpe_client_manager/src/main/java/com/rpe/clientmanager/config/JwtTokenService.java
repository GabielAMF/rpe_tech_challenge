package com.rpe.clientmanager.config;

import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.service.IssuedToken;
import com.rpe.clientmanager.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Issues HS256 JWTs: {@code sub} is the user id, plus {@code preferred_username} and {@code roles}. The claim names
 * live here; SecurityConfig reads {@link #ROLES_CLAIM} to turn the roles back into authorities.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {

    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    @Override
    public IssuedToken issue(AppUser user) {
        // JWT times are whole seconds; truncate so expiresAt matches the exp claim exactly.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(securityProperties.jwt().expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwt().issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("preferred_username", user.getUsername())
                .claim(ROLES_CLAIM, List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
