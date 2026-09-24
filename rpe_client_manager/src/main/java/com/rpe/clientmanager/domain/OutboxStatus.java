package com.rpe.clientmanager.domain;

public enum OutboxStatus {
    /** Waiting to be sent (or to be retried at {@code nextAttemptAt}). */
    PENDING,
    /** Accepted by SQS. The payload has been cleared. */
    SENT,
    /** Gave up after the maximum attempts. Needs manual action to be sent again (see README). */
    FAILED
}
