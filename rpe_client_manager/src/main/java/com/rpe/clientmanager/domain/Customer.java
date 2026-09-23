package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.CustomerAlreadyActiveException;
import com.rpe.clientmanager.exception.InvalidCustomerNameException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A customer. The CPF is set once and never changes. Status transitions:
 * cancel (any → CANCELADO, idempotent), block (any → BLOQUEADO, idempotent), activate (BLOQUEADO/CANCELADO → ATIVO).
 */
@Getter
@Entity
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "cpf", nullable = false, length = 11, updatable = false)
    private String cpf;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status;

    public Customer(String name, Cpf cpf, LocalDate birthDate) {
        this.name = normalizeName(name);
        this.cpf = Objects.requireNonNull(cpf, "cpf").value();
        this.birthDate = Objects.requireNonNull(birthDate, "birthDate");
        this.status = CustomerStatus.ATIVO;
    }

    /** Name and birth date only: the CPF is immutable and status changes have their own methods. */
    public void update(String name, LocalDate birthDate) {
        this.name = normalizeName(name);
        this.birthDate = Objects.requireNonNull(birthDate, "birthDate");
    }

    public Cpf getCpfValue() {
        return new Cpf(cpf);
    }

    public void cancel() {
        this.status = CustomerStatus.CANCELADO;
    }

    public void block() {
        this.status = CustomerStatus.BLOQUEADO;
    }

    public void activate() {
        if (status == CustomerStatus.ATIVO) {
            throw new CustomerAlreadyActiveException(id);
        }
        this.status = CustomerStatus.ATIVO;
    }

    private static String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidCustomerNameException();
        }
        return trimmed;
    }
}
