package com.rpe.clientmanager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** {@code birthDate} is ISO-8601 ("1990-05-20"); {@code cpf} may be formatted ("123.456.789-09"). */
public record CreateCustomerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 20) String cpf,
        @NotNull @Past LocalDate birthDate
) {
}
