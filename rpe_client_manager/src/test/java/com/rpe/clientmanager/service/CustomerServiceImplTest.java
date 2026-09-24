package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.CustomerName;
import com.rpe.clientmanager.domain.Customer;
import com.rpe.clientmanager.domain.CustomerStatus;
import com.rpe.clientmanager.exception.CustomerAlreadyActiveException;
import com.rpe.clientmanager.exception.CustomerNotFoundException;
import com.rpe.clientmanager.exception.DuplicateCpfException;
import com.rpe.clientmanager.exception.InvalidBirthDateException;
import com.rpe.clientmanager.exception.StatusChangeNotAllowedException;
import com.rpe.clientmanager.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private CardProductionPublisher cardProductionPublisher;

    @Mock
    private CardInfoGateway cardInfoGateway;

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, cpfPolicy, birthDatePolicy,
                cardProductionPublisher, cardInfoGateway, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private final UUID id = UUID.randomUUID();

    @Test
    void findByIdThrowsWhenMissing() {
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id)).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void createChecksRulesThenSavesAtivoCustomer() {
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = customerService.create(new CustomerName("Maria"), CPF, BIRTH_DATE, "score=780");

        verify(birthDatePolicy).validate(BIRTH_DATE);
        verify(cpfPolicy).ensureAvailable(CPF);
        assertThat(created.getStatus()).isEqualTo(CustomerStatus.ATIVO);
        assertThat(created.getCpf()).isEqualTo("12345678909");
    }

    @Test
    void createDoesNotSaveWhenCpfIsTaken() {
        doThrow(new DuplicateCpfException(CPF)).when(cpfPolicy).ensureAvailable(CPF);

        assertThatThrownBy(() -> customerService.create(new CustomerName("Maria"), CPF, BIRTH_DATE, "score=780"))
                .isInstanceOf(DuplicateCpfException.class);
        verify(customerRepository, never()).saveAndFlush(any());
        verifyNoInteractions(cardProductionPublisher);
    }

    @Test
    void createPublishesCardProductionRequestAfterSaving() {
        UUID newId = UUID.randomUUID();
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", newId);
            return saved;
        });

        customerService.create(new CustomerName("Maria Silva"), CPF, BIRTH_DATE, "score=780");

        InOrder order = inOrder(customerRepository, cardProductionPublisher);
        order.verify(customerRepository).saveAndFlush(any(Customer.class));
        ArgumentCaptor<CardProductionRequested> event = ArgumentCaptor.forClass(CardProductionRequested.class);
        order.verify(cardProductionPublisher).publish(event.capture());
        assertThat(event.getValue().eventId()).isNotNull();
        assertThat(event.getValue().eventType()).isEqualTo("CARD_PRODUCTION_REQUESTED");
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
        assertThat(event.getValue().customerId()).isEqualTo(newId);
        assertThat(event.getValue().customerName()).isEqualTo("Maria Silva");
        assertThat(event.getValue().cpf()).isEqualTo("12345678909");
        assertThat(event.getValue().creditInfo()).isEqualTo("score=780");
    }

    @Test
    void getDetailsCombinesCustomerAndCardLookup() {
        Customer customer = customer(id);
        CardLookup lookup = CardLookup.unavailable();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(cardInfoGateway.findByCustomerId(id)).thenReturn(lookup);

        CustomerDetails details = customerService.getDetails(id);

        assertThat(details.customer()).isSameAs(customer);
        assertThat(details.card()).isSameAs(lookup);
    }

    @Test
    void getDetailsDoesNotCallCardProcessorForUnknownCustomer() {
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getDetails(id)).isInstanceOf(CustomerNotFoundException.class);
        verifyNoInteractions(cardInfoGateway);
    }

    @Test
    void createDoesNotTouchCpfOrDatabaseWhenBirthDateIsInvalid() {
        doThrow(new InvalidBirthDateException()).when(birthDatePolicy).validate(BIRTH_DATE);

        assertThatThrownBy(() -> customerService.create(new CustomerName("Maria"), CPF, BIRTH_DATE, "score=780"))
                .isInstanceOf(InvalidBirthDateException.class);
        verifyNoInteractions(cpfPolicy, customerRepository, cardProductionPublisher);
    }

    @Test
    void updateWithoutStatusChangesDataButKeepsStatus() {
        Customer customer = customer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer updated = customerService.update(id, new CustomerName("Maria Souza"), NEW_BIRTH_DATE, null);

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

        Customer updated = customerService.update(id, new CustomerName("Maria"), BIRTH_DATE, CustomerStatus.BLOQUEADO);

        assertThat(updated.getStatus()).isEqualTo(CustomerStatus.BLOQUEADO);
    }

    @Test
    void updateWithCurrentStatusChangesNothing() {
        Customer customer = cancelledCustomer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer updated = customerService.update(id, new CustomerName("Maria"), BIRTH_DATE, CustomerStatus.CANCELADO);

        assertThat(updated.getStatus()).isEqualTo(CustomerStatus.CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(value = CustomerStatus.class, names = {"ATIVO", "CANCELADO"})
    void updateRejectsStatusChangesOtherThanBlockingAndChangesNothing(CustomerStatus requested) {
        Customer customer = new Customer(new CustomerName("Maria"), CPF, BIRTH_DATE);
        customer.block();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.update(id, new CustomerName("Other Name"), NEW_BIRTH_DATE, requested))
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
