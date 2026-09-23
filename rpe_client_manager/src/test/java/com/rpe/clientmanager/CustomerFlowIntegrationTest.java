package com.rpe.clientmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.clientmanager.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole customer lifecycle against the real database, with a real token.
 * Needs the infrastructure from docker-compose.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    // A random CPF starting with "TST" so it can't collide with real data and is easy to spot.
    private final String cpf = "TST" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"admin12345\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("accessToken").asText();
    }

    @AfterEach
    void removeTestCustomer() {
        customerRepository.findByCpf(cpf).ifPresent(customerRepository::delete);
    }

    @Test
    void customerLifecycle() throws Exception {
        JsonNode created = json(perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \" Maria Silva \", \"cpf\": \"%s\", \"birthDate\": \"1990-05-20\"}".formatted(cpf.toLowerCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.status").value("ATIVO")));
        String id = created.get("id").asText();

        perform(get("/api/v1/customers/{id}", id)).andExpect(status().isOk());

        perform(put("/api/v1/customers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Maria Souza\", \"birthDate\": \"1990-05-20\", \"status\": \"BLOQUEADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.status").value("BLOQUEADO"));

        perform(delete("/api/v1/customers/{id}", id)).andExpect(status().isNoContent());
        perform(delete("/api/v1/customers/{id}", id)).andExpect(status().isNoContent());

        perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Other\", \"cpf\": \"%s\", \"birthDate\": \"1990-05-20\"}".formatted(cpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCELLED_CUSTOMER_EXISTS"))
                .andExpect(jsonPath("$.customerId").value(id));

        perform(post("/api/v1/customers/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
        perform(post("/api/v1/customers/{id}/activate", id))
                .andExpect(status().isUnprocessableEntity());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
