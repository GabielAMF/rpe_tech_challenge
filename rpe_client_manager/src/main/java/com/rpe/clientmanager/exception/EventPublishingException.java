package com.rpe.clientmanager.exception;

import java.util.UUID;

/**
 * An outbox event couldn't be sent to the message broker. Raised by the outbox relay, never in an HTTP request:
 * the relay retries the event later.
 */
public class EventPublishingException extends CustomException {

    public EventPublishingException(UUID eventId, String reason, Throwable cause) {
        super(ErrorCode.EVENT_PUBLISHING_FAILED, "Could not publish event " + eventId + ": " + reason);
        if (cause != null) {
            initCause(cause);
        }
    }
}
