package com.rpe.clientmanager.messaging;

import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.exception.EventPublishingException;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsOutboxEventSenderTest {

    private static final OutboxEvent EVENT = new OutboxEvent(UUID.randomUUID(), CardProductionRequested.EVENT_TYPE,
            UUID.randomUUID(), "{\"cpf\":\"12345678909\"}", Instant.now());

    @Mock
    private SqsTemplate sqsTemplate;

    private SqsOutboxEventSender sender(Duration timeout) {
        return new SqsOutboxEventSender(sqsTemplate, "test-queue", timeout);
    }

    @Test
    void completesWhenSqsAcceptsTheMessage() {
        when(sqsTemplate.sendAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        assertThatCode(() -> sender(Duration.ofSeconds(1)).send(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void failedSendThrowsWithoutThePayload() {
        when(sqsTemplate.sendAsync(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("sqs down")));

        assertThatThrownBy(() -> sender(Duration.ofSeconds(1)).send(EVENT))
                .isInstanceOf(EventPublishingException.class)
                .hasMessageNotContaining("12345678909");
    }

    @Test
    void slowSendTimesOutInsteadOfHoldingTheLock() {
        when(sqsTemplate.sendAsync(any())).thenReturn(new CompletableFuture<SendResult<Object>>());

        assertThatThrownBy(() -> sender(Duration.ofMillis(50)).send(EVENT))
                .isInstanceOf(EventPublishingException.class);
    }

    @Test
    void unknownEventTypeIsRejected() {
        OutboxEvent other = new OutboxEvent(UUID.randomUUID(), "SOMETHING_ELSE", UUID.randomUUID(), "{}", Instant.now());

        assertThatThrownBy(() -> sender(Duration.ofSeconds(1)).send(other)).isInstanceOf(EventPublishingException.class);
        verifyNoInteractions(sqsTemplate);
    }
}
