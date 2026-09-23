package com.rpe.catalog.service;

import com.rpe.catalog.domain.ProductName;
import com.rpe.catalog.exception.CancelledProductExistsException;
import com.rpe.catalog.exception.DuplicateProductNameException;
import com.rpe.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.rpe.catalog.ProductFixtures.cancelledProduct;
import static com.rpe.catalog.ProductFixtures.product;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductNamePolicyTest {

    private static final ProductName GOLD = new ProductName("Gold");

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductNamePolicy policy;

    private final UUID id = UUID.randomUUID();

    @Test
    void newProductMayUseUnusedName() {
        when(productRepository.findByName("GOLD")).thenReturn(Optional.empty());

        assertThatCode(() -> policy.ensureAvailableForNewProduct(GOLD)).doesNotThrowAnyException();
    }

    @Test
    void newProductMayNotUseNameOfActiveProduct() {
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(product(UUID.randomUUID(), "Gold")));

        assertThatThrownBy(() -> policy.ensureAvailableForNewProduct(GOLD))
                .isInstanceOf(DuplicateProductNameException.class);
    }

    @Test
    void newProductMayNotUseNameOfCancelledProduct() {
        UUID cancelledId = UUID.randomUUID();
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(cancelledProduct(cancelledId, "Gold")));

        assertThatThrownBy(() -> policy.ensureAvailableForNewProduct(GOLD))
                .isInstanceOf(CancelledProductExistsException.class)
                .hasFieldOrPropertyWithValue("productId", cancelledId);
    }

    @Test
    void renamedProductMayKeepItsOwnName() {
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(product(id, "Gold")));

        assertThatCode(() -> policy.ensureAvailableForRename(GOLD, id)).doesNotThrowAnyException();
    }

    @Test
    void renamedProductMayNotTakeNameOfAnotherActiveProduct() {
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(product(UUID.randomUUID(), "Gold")));

        assertThatThrownBy(() -> policy.ensureAvailableForRename(GOLD, id))
                .isInstanceOf(DuplicateProductNameException.class);
    }

    @Test
    void renamedProductMayNotTakeNameOfAnotherCancelledProduct() {
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(cancelledProduct(UUID.randomUUID(), "Gold")));

        assertThatThrownBy(() -> policy.ensureAvailableForRename(GOLD, id))
                .isInstanceOf(CancelledProductExistsException.class);
    }
}
