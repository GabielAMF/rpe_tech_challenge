package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.config.ClockConfig;
import com.rpe.clientmanager.config.SecurityConfig;
import com.rpe.clientmanager.config.SecurityProblemHandler;
import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.domain.UserRole;
import com.rpe.clientmanager.domain.Username;
import com.rpe.clientmanager.exception.InvalidCredentialsException;
import com.rpe.clientmanager.service.AuthService;
import com.rpe.clientmanager.service.IssuedToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SecurityProblemHandler.class, ClockConfig.class})
class AuthControllerTest {

    private static final String NEW_USER = """
            {"username": "bob", "password": "secret-pass", "role": "USER"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginIsPublicAndReturnsBearerToken() throws Exception {
        when(authService.login(new Username("admin"), "secret-pass"))
                .thenReturn(new IssuedToken("jwt-value", Instant.now().plus(Duration.ofHours(1))));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "secret-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void loginWithWrongCredentialsReturns401() throws Exception {
        when(authService.login(any(Username.class), anyString())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginRejectsBlankFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": " ", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401InProblemFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/users").contentType(MediaType.APPLICATION_JSON).content(NEW_USER))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/users")
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_USER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void everyOtherEndpointRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUserIsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanCreateUser() throws Exception {
        AppUser created = new AppUser(new Username("bob"), "hash", UserRole.USER);
        when(authService.createUser(new Username("bob"), "secret-pass", UserRole.USER)).thenReturn(created);

        mockMvc.perform(post("/api/v1/auth/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_USER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createUserRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "bob", "password": "short", "role": "USER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
