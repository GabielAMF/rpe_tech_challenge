package com.rpe.catalog.service;

import com.rpe.catalog.controller.dto.CreateProductRequest;
import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.controller.dto.UpdateProductRequest;
import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductStatus;
import com.rpe.catalog.repository.ProductRepository;
import com.rpe.catalog.service.exception.CancelledProductExistsException;
import com.rpe.catalog.service.exception.DuplicateProductNameException;
import com.rpe.catalog.service.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private final UUID id = UUID.randomUUID();

    @Test
    void createSavesNewProductAsAtivoWithNormalizedName() {
        when(productRepository.findByName("BLACK CARD")).thenReturn(Optional.empty());
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(new CreateProductRequest("  Black Card ", "Gold card"));

        assertThat(response.name()).isEqualTo("BLACK CARD");
        assertThat(response.description()).isEqualTo("Gold card");
        assertThat(response.status()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void createRejectsNameOfActiveProduct() {
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(productWithId(UUID.randomUUID(), "Gold")));

        assertThatThrownBy(() -> productService.create(new CreateProductRequest(" gold", null)))
                .isInstanceOf(DuplicateProductNameException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsNameOfCancelledProduct() {
        Product cancelled = productWithId(UUID.randomUUID(), "Gold");
        cancelled.cancel();
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> productService.create(new CreateProductRequest("Gold", null)))
                .isInstanceOf(CancelledProductExistsException.class)
                .hasMessageContaining("cancelled");
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateChangesAllFields() {
        Product product = new Product("Gold", "Old description");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.findByName("PLATINUM")).thenReturn(Optional.empty());

        ProductResponse response = productService.update(id,
                new UpdateProductRequest("Platinum", "New description", ProductStatus.CANCELADO));

        assertThat(response.name()).isEqualTo("PLATINUM");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.status()).isEqualTo(ProductStatus.CANCELADO);
    }

    @Test
    void updateKeepsItsOwnName() {
        Product product = productWithId(id, "Gold");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.findByName("GOLD")).thenReturn(Optional.of(product));

        ProductResponse response = productService.update(id,
                new UpdateProductRequest("gold", "New description", ProductStatus.ATIVO));

        assertThat(response.description()).isEqualTo("New description");
    }

    @Test
    void updateRejectsNameOfAnotherActiveProduct() {
        when(productRepository.findById(id)).thenReturn(Optional.of(productWithId(id, "Gold")));
        when(productRepository.findByName("PLATINUM"))
                .thenReturn(Optional.of(productWithId(UUID.randomUUID(), "Platinum")));

        assertThatThrownBy(() -> productService.update(id,
                new UpdateProductRequest("Platinum", null, ProductStatus.ATIVO)))
                .isInstanceOf(DuplicateProductNameException.class);
    }

    @Test
    void updateRejectsNameOfAnotherCancelledProduct() {
        Product cancelled = productWithId(UUID.randomUUID(), "Platinum");
        cancelled.cancel();
        when(productRepository.findById(id)).thenReturn(Optional.of(productWithId(id, "Gold")));
        when(productRepository.findByName("PLATINUM")).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> productService.update(id,
                new UpdateProductRequest("Platinum", null, ProductStatus.ATIVO)))
                .isInstanceOf(CancelledProductExistsException.class);
    }

    @Test
    void cancelMarksProductAsCancelado() {
        Product product = new Product("Gold", null);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.cancel(id);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.CANCELADO);
    }

    private static Product productWithId(UUID id, String name) {
        Product product = new Product(name, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
