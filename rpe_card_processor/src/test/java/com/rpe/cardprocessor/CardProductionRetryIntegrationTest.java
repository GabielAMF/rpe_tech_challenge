package com.rpe.cardprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.rpe.cardprocessor.client.CachedCatalogGateway;
import com.rpe.cardprocessor.messaging.DeadLetterQueueMonitor;
import com.rpe.cardprocessor.repository.CardRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Retry strategy end to end: with rpe_catalog failing, a card production message is attempted exactly 3 times with
 * growing gaps (exponential backoff through the visibility timeout), then SQS moves it to the DLQ, where the
 * monitor sees it, and no card is created. Uses its own queue + DLQ with the same redrive policy as the real one,
 * and a short backoff (1s, 2s, 4s) so the test stays fast. Needs the infrastructure from docker-compose.yml.
 */
@SpringBootTest
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "integrations.catalog.base-url"))
class CardProductionRetryIntegrationTest {

    private static final String QUEUE = "rpe-card-processor-retry-test-" + UUID.randomUUID();
    private static final String DLQ = QUEUE + "-dlq";
    private static final SqsAsyncClient SQS = SqsAsyncClient.builder()
            .endpointOverride(URI.create(System.getenv().getOrDefault("AWS_SQS_ENDPOINT", "http://localhost:4566")))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();

    /** Runs before the context starts, so the listener finds the queue already created with its redrive policy. */
    @DynamicPropertySource
    static void queues(DynamicPropertyRegistry registry) {
        String dlqUrl = SQS.createQueue(r -> r.queueName(DLQ)).join().queueUrl();
        String dlqArn = SQS.getQueueAttributes(r -> r.queueUrl(dlqUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
                .join().attributes().get(QueueAttributeName.QUEUE_ARN);
        SQS.createQueue(r -> r.queueName(QUEUE).attributes(Map.of(QueueAttributeName.REDRIVE_POLICY,
                "{\"deadLetterTargetArn\":\"" + dlqArn + "\",\"maxReceiveCount\":\"3\"}"))).join();

        registry.add("app.sqs.client-manager-queue", () -> QUEUE);
        registry.add("app.sqs.client-manager-dlq", () -> DLQ);
        registry.add("app.sqs.retry.initial-backoff", () -> "1s");
        registry.add("app.sqs.retry.multiplier", () -> "2");
        registry.add("app.sqs.retry.max-backoff", () -> "10s");
    }

    @AfterAll
    static void deleteQueues() {
        for (String queue : List.of(QUEUE, DLQ)) {
            SQS.getQueueUrl(r -> r.queueName(queue))
                    .thenCompose(url -> SQS.deleteQueue(r -> r.queueUrl(url.queueUrl())))
                    .join();
        }
    }

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void failingMessageIsRetriedWithBackoffThenDeadLettered() throws Exception {
        cacheManager.getCache(CachedCatalogGateway.CACHE).evict(TestCardProperties.DEFAULT_PRODUCT_ID);
        String productUrl = "/api/v1/products/" + TestCardProperties.DEFAULT_PRODUCT_ID;
        stubFor(get(productUrl).willReturn(aResponse().withStatus(500)));
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        sqsTemplate.send(to -> to.queue(QUEUE).payload("""
                {"eventId": "%s", "eventType": "CARD_PRODUCTION_REQUESTED", "occurredAt": "2026-09-24T12:00:00Z",
                 "customerId": "%s", "customerName": "Maria Silva", "cpf": "12345678909", "creditInfo": "score=780"}
                """.formatted(eventId, customerId)));

        Message dead = awaitDeadLetter();
        assertThat(objectMapper.readTree(dead.body()).get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(cardRepository.findByCustomerId(customerId)).isEmpty();

        // One catalog call per attempt: exactly 3 attempts, each after a longer wait (>= 1s, then >= 2s).
        List<Instant> attempts = getAllServeEvents().stream()
                .filter(event -> event.getRequest().getUrl().equals(productUrl))
                .map(ServeEvent::getRequest)
                .map(request -> request.getLoggedDate().toInstant())
                .sorted(Comparator.naturalOrder())
                .toList();
        assertThat(attempts).hasSize(3);
        assertThat(Duration.between(attempts.get(0), attempts.get(1))).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
        assertThat(Duration.between(attempts.get(1), attempts.get(2))).isGreaterThanOrEqualTo(Duration.ofSeconds(2));

        assertThat(new DeadLetterQueueMonitor(SQS, DLQ).countMessages()).isEqualTo(1);
    }

    /** Peeks at the DLQ (visibility 0, not deleted) until the message arrives. */
    private Message awaitDeadLetter() {
        String dlqUrl = SQS.getQueueUrl(r -> r.queueName(DLQ)).join().queueUrl();
        Instant deadline = Instant.now().plus(Duration.ofSeconds(40));
        while (Instant.now().isBefore(deadline)) {
            List<Message> messages = SQS.receiveMessage(r -> r.queueUrl(dlqUrl).waitTimeSeconds(2).visibilityTimeout(0))
                    .join().messages();
            if (!messages.isEmpty()) {
                return messages.get(0);
            }
        }
        return fail("The message never reached the DLQ " + DLQ);
    }
}
