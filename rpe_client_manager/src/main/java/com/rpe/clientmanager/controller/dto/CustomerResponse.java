package com.rpe.clientmanager.controller.dto;

import com.rpe.clientmanager.domain.CustomerStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String cpf,
        LocalDate birthDate,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
