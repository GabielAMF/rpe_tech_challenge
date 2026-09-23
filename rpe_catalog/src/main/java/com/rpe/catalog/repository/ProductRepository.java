package com.rpe.catalog.repository;

import com.rpe.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Products are soft-deleted (status CANCELADO), never removed: don't call the inherited delete methods.
 * Extending JpaRepository is a deliberate trade-off, see "Repositories" in the README.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByName(String name);
}
