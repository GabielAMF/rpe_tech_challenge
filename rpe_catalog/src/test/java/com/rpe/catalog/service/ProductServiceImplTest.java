package com.rpe.catalog.service;

import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductName;
import com.rpe.catalog.domain.ProductStatus;
import com.rpe.catalog.exception.DuplicateProductNameException;
import com.rpe.catalog.exception.ProductAlreadyActiveException;
import com.rpe.catalog.exception.ProductNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final ProductName GOLD = new ProductName("Gold");
    private static final ProductName PLATINUM = new ProductName("Platinum");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductNamePolicy productNamePolicy;

    @InjectMocks
    private ProductServiceImpl productService;

    private final UUID id = UUID.randomUUID();

    @Test
    void findByIdReturnsProduct() {
        Product product = product(id, "Gold");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertThat(productService.findById(id)).isSameAs(product);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createChecksNameThenSavesAtivoProduct() {
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create(GOLD, "Gold card");

        verify(productNamePolicy).ensureAvailableForNewProduct(GOLD);
        assertThat(created.getName()).isEqualTo("GOLD");
        assertThat(created.getDescription()).isEqualTo("Gold card");
        assertThat(created.getStatus()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void createDoesNotSaveWhenPolicyRejectsName() {
        doThrow(new DuplicateProductNameException("GOLD")).when(productNamePolicy).ensureAvailableForNewProduct(GOLD);

        assertThatThrownBy(() -> productService.create(GOLD, null))
                .isInstanceOf(DuplicateProductNameException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateChecksNameThenChangesNameAndDescription() {
        Product product = product(id, "Gold", "Old description");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        Product updated = productService.update(id, PLATINUM, "New description");

        verify(productNamePolicy).ensureAvailableForRename(PLATINUM, id);
        verify(productRepository).flush();
        assertThat(updated.getName()).isEqualTo("PLATINUM");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void updateDoesNotChangeProductWhenPolicyRejectsName() {
        Product product = product(id, "Gold");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        doThrow(new DuplicateProductNameException("PLATINUM")).when(productNamePolicy).ensureAvailableForRename(PLATINUM, id);

        assertThatThrownBy(() -> productService.update(id, PLATINUM, null))
                .isInstanceOf(DuplicateProductNameException.class);
        assertThat(product.getName()).isEqualTo("GOLD");
    }

    @Test
    void updateThrowsWhenMissing() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(id, GOLD, null))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void cancelMarksProductAsCancelado() {
        Product product = product(id, "Gold");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.cancel(id);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.CANCELADO);
    }

    @Test
    void cancelIsIdempotentForAlreadyCancelledProduct() {
        Product product = cancelledProduct(id, "Gold");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.cancel(id);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.CANCELADO);
    }

    @Test
    void activateReactivatesCancelledProduct() {
        when(productRepository.findById(id)).thenReturn(Optional.of(cancelledProduct(id, "Gold")));

        Product activated = productService.activate(id);

        assertThat(activated.getStatus()).isEqualTo(ProductStatus.ATIVO);
        verify(productRepository).flush();
    }

    @Test
    void activateRejectsProductThatIsAlreadyActive() {
        when(productRepository.findById(id)).thenReturn(Optional.of(product(id, "Gold")));

        assertThatThrownBy(() -> productService.activate(id))
                .isInstanceOf(ProductAlreadyActiveException.class);
        verify(productRepository, never()).flush();
    }

    @Test
    void activateThrowsWhenMissing() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.activate(id))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
