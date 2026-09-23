package com.rpe.clientmanager.controller.dto;

import com.rpe.clientmanager.domain.CustomerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The CPF is not here on purpose: it can't be changed. {@code status} is optional and may only be BLOQUEADO
 * (DELETE cancels, POST .../activate activates).
 */
public record UpdateCustomerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @Past LocalDate birthDate,
        CustomerStatus status
) {
}
