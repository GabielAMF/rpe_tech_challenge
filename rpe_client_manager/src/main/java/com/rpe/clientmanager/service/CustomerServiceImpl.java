package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.Cpf;
import com.rpe.clientmanager.domain.Customer;
import com.rpe.clientmanager.domain.CustomerStatus;
import com.rpe.clientmanager.exception.CustomerNotFoundException;
import com.rpe.clientmanager.exception.StatusChangeNotAllowedException;
import com.rpe.clientmanager.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Customer data is personal: logs carry the id and masked CPF only, never the name, birth date or credit info.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerCpfPolicy cpfPolicy;
    private final BirthDatePolicy birthDatePolicy;
    private final CardProductionPublisher cardProductionPublisher;
    private final CardInfoGateway cardInfoGateway;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return getCustomer(id);
    }

    /** Not transactional on purpose: the HTTP call to rpe_card_processor must not hold a database transaction. */
    @Override
    public CustomerDetails getDetails(UUID id) {
        Customer customer = getCustomer(id);
        return new CustomerDetails(customer, cardInfoGateway.findByCustomerId(id));
    }

    @Override
    @Transactional
    public Customer create(String name, Cpf cpf, LocalDate birthDate, String creditInfo) {
        birthDatePolicy.validate(birthDate);
        cpfPolicy.ensureAvailable(cpf);
        // saveAndFlush so the id and audit timestamps exist before the event is built and the customer returned.
        Customer customer = customerRepository.saveAndFlush(new Customer(name, cpf, birthDate));
        // Inside the transaction: if publishing throws, the customer is rolled back (see SqsCardProductionPublisher).
        cardProductionPublisher.publish(CardProductionRequested.of(customer, creditInfo, clock.instant()));
        log.info("Created customer id={} cpf={} and requested card production", customer.getId(), cpf.masked());
        return customer;
    }

    @Override
    @Transactional
    public Customer update(UUID id, String name, LocalDate birthDate, CustomerStatus status) {
        Customer customer = getCustomer(id);
        boolean block = shouldBlock(customer, status);
        birthDatePolicy.validate(birthDate);
        customer.update(name, birthDate);
        if (block) {
            customer.block();
        }
        customerRepository.flush();
        log.info("Updated customer id={} status={}", id, customer.getStatus());
        return customer;
    }

    @Override
    @Transactional
    public void cancel(UUID id) {
        Customer customer = getCustomer(id);
        customer.cancel();
        log.info("Cancelled customer id={}", id);
    }

    @Override
    @Transactional
    public Customer activate(UUID id) {
        Customer customer = getCustomer(id);
        customer.activate();
        // flush so updatedAt on the returned customer is the one stored.
        customerRepository.flush();
        log.info("Activated customer id={}", id);
        return customer;
    }

    /** No status, or the current one, changes nothing; BLOQUEADO blocks; anything else is rejected. */
    private static boolean shouldBlock(Customer customer, CustomerStatus requested) {
        if (requested == null || requested == customer.getStatus()) {
            return false;
        }
        if (requested != CustomerStatus.BLOQUEADO) {
            throw new StatusChangeNotAllowedException(requested);
        }
        return true;
    }

    private Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
