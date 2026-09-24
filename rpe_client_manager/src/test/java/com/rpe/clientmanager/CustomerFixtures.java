package com.rpe.clientmanager;

import com.rpe.clientmanager.domain.CustomerName;
import com.rpe.clientmanager.domain.Cpf;
import com.rpe.clientmanager.domain.Customer;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Builds customers as if they had been loaded from the database (id and audit timestamps set). */
public final class CustomerFixtures {

    public static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
    public static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);
    public static final Cpf CPF = new Cpf("123.456.789-09");

    private CustomerFixtures() {
    }

    public static Customer customer(UUID id) {
        Customer customer = new Customer(new CustomerName("Maria Silva"), CPF, BIRTH_DATE);
        ReflectionTestUtils.setField(customer, "id", id);
        ReflectionTestUtils.setField(customer, "createdAt", NOW);
        ReflectionTestUtils.setField(customer, "updatedAt", NOW);
        return customer;
    }

    public static Customer cancelledCustomer(UUID id) {
        Customer customer = customer(id);
        customer.cancel();
        return customer;
    }
}
