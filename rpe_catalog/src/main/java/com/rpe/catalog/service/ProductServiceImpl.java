package com.rpe.catalog.service;

import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductName;
import com.rpe.catalog.exception.ProductNotFoundException;
import com.rpe.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductNamePolicy productNamePolicy;

    @Override
    @Transactional(readOnly = true)
    public Product findById(UUID id) {
        log.debug("Loading product id={} from the database", id);
        return getProduct(id);
    }

    @Override
    @Transactional
    public Product create(ProductName name, String description) {
        productNamePolicy.ensureAvailableForNewProduct(name);
        // saveAndFlush so the audit timestamps are set before the product is returned.
        Product product = productRepository.saveAndFlush(new Product(name, description));
        log.info("Created product id={} name='{}'", product.getId(), product.getName());
        return product;
    }

    @Override
    @Transactional
    public Product update(UUID id, ProductName name, String description) {
        Product product = getProduct(id);
        productNamePolicy.ensureAvailableForRename(name, id);
        product.update(name, description);
        productRepository.flush();
        log.info("Updated product id={} name='{}'", id, product.getName());
        return product;
    }

    /**
     * Soft delete: the product is marked CANCELADO instead of being removed, so its history
     * and any references from other services stay valid.
     */
    @Override
    @Transactional
    public void cancel(UUID id) {
        Product product = getProduct(id);
        product.cancel();
        log.info("Cancelled product id={}", id);
    }

    @Override
    @Transactional
    public Product activate(UUID id) {
        Product product = getProduct(id);
        product.activate();
        // flush so updatedAt on the returned product is the one stored.
        productRepository.flush();
        log.info("Activated product id={}", id);
        return product;
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
