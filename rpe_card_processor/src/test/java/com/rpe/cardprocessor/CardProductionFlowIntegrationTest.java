package com.rpe.cardprocessor;

import com.rpe.cardprocessor.client.CachedCatalogGateway;
import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.domain.CardStatus;
import com.rpe.cardprocessor.repository.CardRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end: a card production message on (LocalStack) SQS is consumed, the product is read from rpe_catalog
 * (WireMock), the card is stored with its sensitive fields encrypted, and it can then be read (masked) through
 * {@code GET /api/v1/customers/{customerId}/card}, the endpoint rpe_client_manager calls. Uses a queue of its own, created for
 * this run and deleted afterwards, so messages left by an earlier failed run can't interfere.
 * Needs the infrastructure from docker-compose.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "integrations.catalog.base-url"))
class CardProductionFlowIntegrationTest {

    private static final String QUEUE = "rpe-card-processor-test-" + UUID.randomUUID();

    @DynamicPropertySource
    static void queue(DynamicPropertyRegistry registry) {
        registry.add("app.sqs.client-manager-queue", () -> QUEUE);
    }

    @AfterAll
    static void deleteQueue(@Autowired SqsAsyncClient sqs) {
        sqs.getQueueUrl(r -> r.queueName(QUEUE))
                .thenCompose(url -> sqs.deleteQueue(r -> r.queueUrl(url.queueUrl())))
                .join();
    }

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void stubCatalog() {
        cacheManager.getCache(CachedCatalogGateway.CACHE).evict(TestCardProperties.DEFAULT_PRODUCT_ID);
        stubFor(get("/api/v1/products/" + TestCardProperties.DEFAULT_PRODUCT_ID).willReturn(okJson("""
                {"id": "%s", "name": "GOLD", "description": "Standard card", "status": "ATIVO",
                 "createdAt": "2026-09-23T12:00:00Z", "updatedAt": "2026-09-23T12:00:00Z"}
                """.formatted(TestCardProperties.DEFAULT_PRODUCT_ID))));
    }

    @AfterEach
    void removeCard() {
        cardRepository.findByCustomerId(customerId).ifPresent(cardRepository::delete);
    }

    @Test
    void messageBecomesAnEncryptedCardOnlyOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        // The same JSON rpe_client_manager sends (plain JSON, no Java type header).
        String message = """
                {"eventId": "%s", "eventType": "CARD_PRODUCTION_REQUESTED", "occurredAt": "2026-09-23T12:00:00Z",
                 "customerId": "%s", "customerName": "Maria Silva", "cpf": "12345678909", "creditInfo": "score=780"}
                """.formatted(eventId, customerId);

        // Before the message is processed, the customer has no card.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/customers/{customerId}/card", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));

        sqsTemplate.send(to -> to.queue(QUEUE).payload(message));
        Card card = awaitCard();

        assertThat(card.getSourceEventId()).isEqualTo(eventId);
        assertThat(card.getHolderName()).isEqualTo("Maria Silva");
        assertThat(card.getStatus()).isEqualTo(CardStatus.ATIVO);
        assertThat(card.getProduct().name()).isEqualTo("GOLD");
        assertThat(card.getNumber()).hasSize(16).startsWith(card.getOperator().getNumberPrefix());
        assertThat(card.getMaskedNumber()).endsWith(card.getNumber().substring(12));

        // In the database the sensitive columns are ciphertext.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT number_encrypted, expiry_encrypted, cvv_encrypted FROM card WHERE id = ?", card.getId());
        assertThat((String) row.get("number_encrypted")).doesNotContain(card.getNumber());
        assertThat((String) row.get("expiry_encrypted")).doesNotContain(card.getExpiry().toString());
        assertThat((String) row.get("cvv_encrypted")).isNotEqualTo(card.getCvv());

        // rpe_client_manager's view of the card: masked number and product, nothing sensitive.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/customers/{customerId}/card", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(card.getId().toString()))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.maskedNumber").value(card.getMaskedNumber()))
                .andExpect(jsonPath("$.product.name").value("GOLD"))
                .andExpect(jsonPath("$.product.status").value("ATIVO"))
                .andExpect(content().string(not(containsString(card.getNumber()))));

        // Delivering the same message again doesn't create a second card.
        sqsTemplate.send(to -> to.queue(QUEUE).payload(message));
        sleep(Duration.ofSeconds(3));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM card WHERE customer_id = ?", Long.class, customerId))
                .isEqualTo(1L);
    }

    private Card awaitCard() {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline)) {
            Optional<Card> card = cardRepository.findByCustomerId(customerId);
            if (card.isPresent()) {
                return card.get();
            }
            sleep(Duration.ofMillis(200));
        }
        return fail("No card produced for customer " + customerId);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
