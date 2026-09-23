package com.rpe.catalog.domain;

import com.rpe.catalog.exception.InvalidProductNameException;
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

import java.util.Locale;
import java.util.UUID;

@Getter
@Entity
@Table(name = "card_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    public Product(String name, String description) {
        this.name = normalizeName(name);
        this.description = description;
        this.status = ProductStatus.ATIVO;
    }

    public void update(String name, String description, ProductStatus status) {
        this.name = normalizeName(name);
        this.description = description;
        this.status = status;
    }

    /**
     * Product names are stored trimmed and upper-cased, so " gold " and "GOLD" are the same product.
     * A name that is null or empty after trimming is rejected.
     */
    public static String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidProductNameException();
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public void cancel() {
        this.status = ProductStatus.CANCELADO;
    }
}
