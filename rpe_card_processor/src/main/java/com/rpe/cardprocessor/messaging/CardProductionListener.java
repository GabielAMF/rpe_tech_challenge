package com.rpe.cardprocessor.messaging;

import com.rpe.cardprocessor.exception.InvalidCardProductionRequestException;
import com.rpe.cardprocessor.service.CardProductionCommand;
import com.rpe.cardprocessor.service.CardProductionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumes rpe_client_manager's card production requests.
 * <p>
 * A message is acknowledged (deleted) only when this method returns normally. If it throws, e.g. rpe_catalog is
 * down, SQS makes the message visible again after the visibility timeout and retries it; after 3 failed receives
 * the queue's redrive policy moves it to {@code rpe-client-manager-queue-dlq}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardProductionListener {

    private final CardProductionService cardProductionService;

    @SqsListener("${app.sqs.client-manager-queue}")
    public void onMessage(CardProductionRequestedMessage message) {
        if (!CardProductionRequestedMessage.EVENT_TYPE.equals(message.eventType())) {
            log.warn("Ignoring {}: unknown event type", message);
            return;
        }
        validate(message);
        log.info("Received {}", message);
        cardProductionService.produce(new CardProductionCommand(
                message.eventId(), message.customerId(), message.customerName(), message.creditInfo()));
    }

    private static void validate(CardProductionRequestedMessage message) {
        if (message.eventId() == null) {
            throw new InvalidCardProductionRequestException("eventId is missing");
        }
        if (message.customerId() == null) {
            throw new InvalidCardProductionRequestException("customerId is missing");
        }
        if (!StringUtils.hasText(message.customerName())) {
            throw new InvalidCardProductionRequestException("customerName is missing");
        }
    }
}
