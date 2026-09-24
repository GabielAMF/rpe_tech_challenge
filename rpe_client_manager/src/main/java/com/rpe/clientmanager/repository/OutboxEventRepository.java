package com.rpe.clientmanager.repository;

import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Due PENDING events, oldest first, locked until the transaction ends. {@code SKIP LOCKED}: rows another
     * instance is already sending are skipped instead of waited for, so two instances never send the same row.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockDue(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.sentAt < :before")
    int deleteByStatusAndSentAtBefore(@Param("status") OutboxStatus status, @Param("before") Instant before);

    List<OutboxEvent> findByAggregateId(UUID aggregateId);
}
