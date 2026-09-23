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

import java.time.LocalDate;
import java.util.UUID;

/** Customer data is personal: logs carry the id and masked CPF only, never the name or birth date. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerCpfPolicy cpfPolicy;
    private final BirthDatePolicy birthDatePolicy;

    @Override
    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return getCustomer(id);
    }

    @Override
    @Transactional
    public Customer create(String name, Cpf cpf, LocalDate birthDate) {
        birthDatePolicy.validate(birthDate);
        cpfPolicy.ensureAvailable(cpf);
        // saveAndFlush so the audit timestamps are set before the customer is returned.
        Customer customer = customerRepository.saveAndFlush(new Customer(name, cpf, birthDate));
        log.info("Created customer id={} cpf={}", customer.getId(), cpf.masked());
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
