package com.rpe.clientmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.clientmanager.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Duration;
import java.time.Instant;

import java.util.concurrent.ThreadLocalRandom;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole customer lifecycle against the real infrastructure: database, a real token, the card production
 * message on LocalStack SQS, and card info from an in-process WireMock standing in for rpe_card_processor
 * (the real services together are tested by hand, see README "Running"). Needs the infrastructure from
 * docker-compose.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "integrations.card-processor.base-url"))
class CustomerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SqsAsyncClient sqs;

    @Value("${app.sqs.client-manager-queue}")
    private String queue;

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
                .content("""
                        {"name": " Maria Silva ", "cpf": "%s", "birthDate": "1990-05-20", "credit_info": "score=780"}
                        """.formatted(cpf.toLowerCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.status").value("ATIVO")));
        String id = created.get("id").asText();

        // Creating the customer published a card production request, as plain JSON.
        Message message = takeCardProductionMessage(id);
        JsonNode event = objectMapper.readTree(message.body());
        assertThat(event.get("eventType").asText()).isEqualTo("CARD_PRODUCTION_REQUESTED");
        assertThat(event.get("customerName").asText()).isEqualTo("Maria Silva");
        assertThat(event.get("cpf").asText()).isEqualTo(cpf);
        assertThat(event.get("creditInfo").asText()).isEqualTo("score=780");
        assertThat(message.messageAttributes()).doesNotContainKey("JavaType");

        // GET combines the customer with the card from rpe_card_processor (stubbed).
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/customers/" + id + "/card"))
                .willReturn(okJson("""
                        {"cardId": "0c6f8a2e-1b3d-4e5f-8a7b-9c0d1e2f3a4b", "status": "ATIVO",
                         "maskedNumber": "**** **** **** 1234", "createdAt": "2026-09-23T12:00:00Z",
                         "product": {"id": "3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b", "name": "GOLD",
                                     "description": "Gold card", "status": "ATIVO"}}
                        """)));
        perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.cardInfoAvailable").value(true))
                .andExpect(jsonPath("$.card.maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.card.product.name").value("GOLD"));

        perform(put("/api/v1/customers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Maria Souza\", \"birthDate\": \"1990-05-20\", \"status\": \"BLOQUEADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.status").value("BLOQUEADO"));

        perform(delete("/api/v1/customers/{id}", id)).andExpect(status().isNoContent());
        perform(delete("/api/v1/customers/{id}", id)).andExpect(status().isNoContent());

        perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Other", "cpf": "%s", "birthDate": "1990-05-20", "creditInfo": "score=780"}
                        """.formatted(cpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCELLED_CUSTOMER_EXISTS"))
                .andExpect(jsonPath("$.customerId").value(id));

        perform(post("/api/v1/customers/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
        perform(post("/api/v1/customers/{id}/activate", id))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Finds this test's message on the queue and deletes only that one. Other messages are made visible again
     * straight away (visibility timeout 0) so they stay for whoever consumes them.
     */
    private Message takeCardProductionMessage(String customerId) throws Exception {
        String queueUrl = sqs.getQueueUrl(r -> r.queueName(queue)).join().queueUrl();
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            for (Message message : sqs.receiveMessage(r -> r.queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(1)
                    .visibilityTimeout(0)
                    .messageAttributeNames("All")).join().messages()) {
                if (customerId.equals(objectMapper.readTree(message.body()).path("customerId").asText())) {
                    sqs.deleteMessage(r -> r.queueUrl(queueUrl).receiptHandle(message.receiptHandle())).join();
                    return message;
                }
            }
        }
        return fail("No card production message for customer " + customerId);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
