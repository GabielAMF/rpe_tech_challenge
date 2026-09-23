package com.rpe.clientmanager.repository;

import com.rpe.clientmanager.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Customers are soft-deleted (status CANCELADO), never removed: don't call the inherited delete methods.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCpf(String cpf);
}
