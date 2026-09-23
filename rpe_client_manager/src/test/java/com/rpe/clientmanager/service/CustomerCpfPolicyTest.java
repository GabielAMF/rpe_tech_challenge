package com.rpe.clientmanager.service;

import com.rpe.clientmanager.exception.CancelledCustomerExistsException;
import com.rpe.clientmanager.exception.DuplicateCpfException;
import com.rpe.clientmanager.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.rpe.clientmanager.CustomerFixtures.CPF;
import static com.rpe.clientmanager.CustomerFixtures.cancelledCustomer;
import static com.rpe.clientmanager.CustomerFixtures.customer;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCpfPolicyTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerCpfPolicy policy;

    @Test
    void unusedCpfIsAvailable() {
        when(customerRepository.findByCpf("12345678909")).thenReturn(Optional.empty());

        assertThatCode(() -> policy.ensureAvailable(CPF)).doesNotThrowAnyException();
    }

    @Test
    void cpfOfActiveOrBlockedCustomerIsTaken() {
        when(customerRepository.findByCpf("12345678909")).thenReturn(Optional.of(customer(UUID.randomUUID())));

        assertThatThrownBy(() -> policy.ensureAvailable(CPF))
                .isInstanceOf(DuplicateCpfException.class)
                .hasMessageContaining("***.***.***-09")
                .hasMessageNotContaining("12345678909");
    }

    @Test
    void cpfOfCancelledCustomerPointsToReactivation() {
        UUID cancelledId = UUID.randomUUID();
        when(customerRepository.findByCpf("12345678909")).thenReturn(Optional.of(cancelledCustomer(cancelledId)));

        assertThatThrownBy(() -> policy.ensureAvailable(CPF))
                .isInstanceOf(CancelledCustomerExistsException.class)
                .hasFieldOrPropertyWithValue("customerId", cancelledId)
                .hasMessageNotContaining("12345678909");
    }
}
