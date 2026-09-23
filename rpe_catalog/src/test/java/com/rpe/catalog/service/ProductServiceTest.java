package com.rpe.catalog.service;

import com.rpe.catalog.controller.dto.CreateProductRequest;
import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.controller.dto.UpdateProductRequest;
import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductStatus;
import com.rpe.catalog.repository.ProductRepository;
import com.rpe.catalog.service.exception.DuplicateProductNameException;
import com.rpe.catalog.service.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(productRepository.existsByName("BLACK CARD")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(new CreateProductRequest("  Black Card ", "Gold card"));

        assertThat(response.name()).isEqualTo("BLACK CARD");
        assertThat(response.description()).isEqualTo("Gold card");
        assertThat(response.status()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void createRejectsDuplicateName() {
        when(productRepository.existsByName("GOLD")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(new CreateProductRequest(" gold", null)))
                .isInstanceOf(DuplicateProductNameException.class);
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
        when(productRepository.existsByNameAndIdNot("PLATINUM", id)).thenReturn(false);

        ProductResponse response = productService.update(id,
                new UpdateProductRequest("Platinum", "New description", ProductStatus.CANCELADO));

        assertThat(response.name()).isEqualTo("PLATINUM");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.status()).isEqualTo(ProductStatus.CANCELADO);
    }

    @Test
    void updateRejectsNameUsedByAnotherProduct() {
        when(productRepository.findById(id)).thenReturn(Optional.of(new Product("Gold", null)));
        when(productRepository.existsByNameAndIdNot("PLATINUM", id)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(id,
                new UpdateProductRequest("Platinum", null, ProductStatus.ATIVO)))
                .isInstanceOf(DuplicateProductNameException.class);
    }

    @Test
    void cancelMarksProductAsCancelado() {
        Product product = new Product("Gold", null);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.cancel(id);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.CANCELADO);
    }
}
