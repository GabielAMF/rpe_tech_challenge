package com.rpe.catalog.domain;

import com.rpe.catalog.exception.ProductAlreadyActiveException;
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

import java.util.Objects;
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

    public Product(ProductName name, String description) {
        this.name = Objects.requireNonNull(name, "name").value();
        this.description = description;
        this.status = ProductStatus.ATIVO;
    }

    /** Changes name and description only; status changes go through {@link #cancel()} and {@link #activate()}. */
    public void update(ProductName name, String description) {
        this.name = Objects.requireNonNull(name, "name").value();
        this.description = description;
    }

    public void cancel() {
        this.status = ProductStatus.CANCELADO;
    }

    public void activate() {
        if (status == ProductStatus.ATIVO) {
            throw new ProductAlreadyActiveException(id);
        }
        this.status = ProductStatus.ATIVO;
    }
}
