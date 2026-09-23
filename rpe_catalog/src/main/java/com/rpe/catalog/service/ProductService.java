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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        log.debug("Loading product id={} from the database", id);
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String name = Product.normalizeName(request.name());
        ensureNameAvailable(name, null);
        // saveAndFlush so the audit timestamps are in the response.
        Product product = productRepository.saveAndFlush(new Product(name, request.description()));
        log.info("Created product id={} name='{}'", product.getId(), product.getName());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = getProduct(id);
        String name = Product.normalizeName(request.name());
        ensureNameAvailable(name, id);
        product.update(name, request.description(), request.status());
        productRepository.flush();
        log.info("Updated product id={} name='{}' status={}", id, product.getName(), product.getStatus());
        return ProductResponse.from(product);
    }

    /**
     * Soft delete: the product is marked CANCELADO instead of being removed, so its history
     * and any references from other services stay valid.
     */
    @Transactional
    public void cancel(UUID id) {
        Product product = getProduct(id);
        product.cancel();
        log.info("Cancelled product id={}", id);
    }

    /**
     * Rejects a name already used by another product. A cancelled owner gets its own exception so the
     * caller knows to reactivate that product. {@code currentId} is the product being updated (null on create).
     */
    private void ensureNameAvailable(String name, UUID currentId) {
        productRepository.findByName(name)
                .filter(existing -> currentId == null || !currentId.equals(existing.getId()))
                .ifPresent(existing -> {
                    if (existing.getStatus() == ProductStatus.CANCELADO) {
                        throw new CancelledProductExistsException(existing.getId(), name);
                    }
                    throw new DuplicateProductNameException(name);
                });
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
