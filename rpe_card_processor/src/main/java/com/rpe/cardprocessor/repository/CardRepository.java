package com.rpe.cardprocessor.repository;

import com.rpe.cardprocessor.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findByCustomerId(UUID customerId);
}
