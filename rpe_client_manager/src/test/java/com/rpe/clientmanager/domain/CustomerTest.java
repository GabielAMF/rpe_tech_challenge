package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.CustomerAlreadyActiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static com.rpe.clientmanager.CustomerFixtures.BIRTH_DATE;
import static com.rpe.clientmanager.CustomerFixtures.CPF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void newCustomerIsAtivoWithTrimmedNameAndNormalizedCpf() {
        Customer customer = new Customer(new CustomerName("  Maria Silva "), CPF, BIRTH_DATE);

        assertThat(customer.getName()).isEqualTo("Maria Silva");
        assertThat(customer.getCpf()).isEqualTo("12345678909");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ATIVO);
    }

    @Test
    void updateChangesNameAndBirthDateOnly() {
        Customer customer = new Customer(new CustomerName("Maria"), CPF, BIRTH_DATE);
        customer.block();

        customer.update(new CustomerName("Maria Souza"), LocalDate.of(1991, 1, 1));

        assertThat(customer.getName()).isEqualTo("Maria Souza");
        assertThat(customer.getBirthDate()).isEqualTo(LocalDate.of(1991, 1, 1));
        assertThat(customer.getCpf()).isEqualTo("12345678909");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOQUEADO);
    }

    @ParameterizedTest
    @EnumSource(CustomerStatus.class)
    void cancelAndBlockWorkFromAnyStatus(CustomerStatus from) {
        Customer customer = inStatus(from);
        customer.block();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOQUEADO);

        customer = inStatus(from);
        customer.cancel();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(value = CustomerStatus.class, names = {"BLOQUEADO", "CANCELADO"})
    void activateBringsBlockedOrCancelledCustomerBack(CustomerStatus from) {
        Customer customer = inStatus(from);

        customer.activate();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ATIVO);
    }

    @Test
    void activateRejectsActiveCustomer() {
        Customer customer = new Customer(new CustomerName("Maria"), CPF, BIRTH_DATE);

        assertThatThrownBy(customer::activate).isInstanceOf(CustomerAlreadyActiveException.class);
    }

    private static Customer inStatus(CustomerStatus status) {
        Customer customer = new Customer(new CustomerName("Maria"), CPF, BIRTH_DATE);
        switch (status) {
            case ATIVO -> { }
            case BLOQUEADO -> customer.block();
            case CANCELADO -> customer.cancel();
        }
        return customer;
    }
}
