package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.Customer;
import com.rpe.clientmanager.domain.CustomerStatus;
import com.rpe.clientmanager.exception.CustomerAlreadyActiveException;
import com.rpe.clientmanager.exception.CustomerNotFoundException;
import com.rpe.clientmanager.exception.DuplicateCpfException;
import com.rpe.clientmanager.exception.InvalidBirthDateException;
import com.rpe.clientmanager.exception.StatusChangeNotAllowedException;
import com.rpe.clientmanager.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.rpe.clientmanager.CustomerFixtures.BIRTH_DATE;
import static com.rpe.clientmanager.CustomerFixtures.CPF;
import static com.rpe.clientmanager.CustomerFixtures.cancelledCustomer;
import static com.rpe.clientmanager.CustomerFixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    private static final LocalDate NEW_BIRTH_DATE = LocalDate.of(1991, 1, 1);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerCpfPolicy cpfPolicy;

    @Mock
    private BirthDatePolicy birthDatePolicy;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private final UUID id = UUID.randomUUID();

    @Test
    void findByIdThrowsWhenMissing() {
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id)).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void createChecksRulesThenSavesAtivoCustomer() {
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = customerService.create("Maria", CPF, BIRTH_DATE);

        verify(birthDatePolicy).validate(BIRTH_DATE);
        verify(cpfPolicy).ensureAvailable(CPF);
        assertThat(created.getStatus()).isEqualTo(CustomerStatus.ATIVO);
        assertThat(created.getCpf()).isEqualTo("12345678909");
    }

    @Test
    void createDoesNotSaveWhenCpfIsTaken() {
        doThrow(new DuplicateCpfException(CPF)).when(cpfPolicy).ensureAvailable(CPF);

        assertThatThrownBy(() -> customerService.create("Maria", CPF, BIRTH_DATE))
                .isInstanceOf(DuplicateCpfException.class);
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDoesNotTouchCpfOrDatabaseWhenBirthDateIsInvalid() {
        doThrow(new InvalidBirthDateException()).when(birthDatePolicy).validate(BIRTH_DATE);

        assertThatThrownBy(() -> customerService.create("Maria", CPF, BIRTH_DATE))
                .isInstanceOf(InvalidBirthDateException.class);
        verifyNoInteractions(cpfPolicy, customerRepository);
    }

    @Test
    void updateWithoutStatusChangesDataButKeepsStatus() {
        Customer customer = customer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer updated = customerService.update(id, "Maria Souza", NEW_BIRTH_DATE, null);

        assertThat(updated.getName()).isEqualTo("Maria Souza");
        assertThat(updated.getBirthDate()).isEqualTo(NEW_BIRTH_DATE);
        assertThat(updated.getStatus()).isEqualTo(CustomerStatus.ATIVO);
        verify(birthDatePolicy).validate(NEW_BIRTH_DATE);
        verify(customerRepository).flush();
    }

    @ParameterizedTest
    @EnumSource(value = CustomerStatus.class, names = {"ATIVO", "CANCELADO"})
    void updateCanBlockFromOtherStatuses(CustomerStatus from) {
        Customer customer = from == CustomerStatus.CANCELADO ? cancelledCustomer(id) : customer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer updated = customerService.update(id, "Maria", BIRTH_DATE, CustomerStatus.BLOQUEADO);

        assertThat(updated.getStatus()).isEqualTo(CustomerStatus.BLOQUEADO);
    }

    @Test
    void updateWithCurrentStatusChangesNothing() {
        Customer customer = cancelledCustomer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer updated = customerService.update(id, "Maria", BIRTH_DATE, CustomerStatus.CANCELADO);

        assertThat(updated.getStatus()).isEqualTo(CustomerStatus.CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(value = CustomerStatus.class, names = {"ATIVO", "CANCELADO"})
    void updateRejectsStatusChangesOtherThanBlockingAndChangesNothing(CustomerStatus requested) {
        Customer customer = new Customer("Maria", CPF, BIRTH_DATE);
        customer.block();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.update(id, "Other Name", NEW_BIRTH_DATE, requested))
                .isInstanceOf(StatusChangeNotAllowedException.class);
        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOQUEADO);
    }

    @Test
    void cancelIsIdempotent() {
        Customer customer = cancelledCustomer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.cancel(id);

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CANCELADO);
    }

    @Test
    void activateReactivatesCancelledCustomer() {
        when(customerRepository.findById(id)).thenReturn(Optional.of(cancelledCustomer(id)));

        Customer activated = customerService.activate(id);

        assertThat(activated.getStatus()).isEqualTo(CustomerStatus.ATIVO);
        verify(customerRepository).flush();
    }

    @Test
    void activateRejectsActiveCustomer() {
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer(id)));

        assertThatThrownBy(() -> customerService.activate(id)).isInstanceOf(CustomerAlreadyActiveException.class);
    }
}
