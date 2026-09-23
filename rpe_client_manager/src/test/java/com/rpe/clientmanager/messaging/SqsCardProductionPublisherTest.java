package com.rpe.clientmanager.messaging;

import com.rpe.clientmanager.exception.CardProductionUnavailableException;
import com.rpe.clientmanager.service.CardProductionRequested;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.rpe.clientmanager.CustomerFixtures.customer;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsCardProductionPublisherTest {

    private static final CardProductionRequested EVENT =
            CardProductionRequested.of(customer(UUID.randomUUID()), "score=780", Instant.now());

    @Mock
    private SqsTemplate sqsTemplate;

    private SqsCardProductionPublisher publisher(Duration timeout) {
        return new SqsCardProductionPublisher(sqsTemplate, "test-queue", timeout);
    }

    @Test
    void completesWhenSqsAcceptsTheMessage() {
        when(sqsTemplate.sendAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertThatCode(() -> publisher(Duration.ofSeconds(1)).publish(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void failedSendBecomesCardProductionUnavailable() {
        when(sqsTemplate.sendAsync(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("sqs down")));

        assertThatThrownBy(() -> publisher(Duration.ofSeconds(1)).publish(EVENT))
                .isInstanceOf(CardProductionUnavailableException.class);
    }

    @Test
    void slowSendTimesOutInsteadOfHoldingTheTransaction() {
        when(sqsTemplate.sendAsync(any())).thenReturn(new CompletableFuture<SendResult<Object>>());

        assertThatThrownBy(() -> publisher(Duration.ofMillis(50)).publish(EVENT))
                .isInstanceOf(CardProductionUnavailableException.class);
    }
}
