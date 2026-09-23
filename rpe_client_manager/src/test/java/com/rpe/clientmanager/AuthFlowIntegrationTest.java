package com.rpe.clientmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.clientmanager.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end against the real database: the bootstrap admin logs in, creates a user, and that user's
 * token is accepted but lacks the ADMIN role. Needs the infrastructure from docker-compose.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    private final String username = "it-" + UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void removeTestUser() {
        userRepository.findByUsername(username).ifPresent(userRepository::delete);
    }

    @Test
    void adminCreatesUserWhoCanLogInButNotCreateUsers() throws Exception {
        String adminToken = login("admin", "admin12345");

        mockMvc.perform(post("/api/v1/auth/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"user-pass-123\", \"role\": \"USER\"}".formatted(username)))
                .andExpect(status().isCreated());

        String userToken = login(username, "user-pass-123");

        mockMvc.perform(post("/api/v1/auth/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"other\", \"password\": \"user-pass-123\", \"role\": \"USER\"}"))
                .andExpect(status().isForbidden());
    }

    private String login(String user, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(user, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}
