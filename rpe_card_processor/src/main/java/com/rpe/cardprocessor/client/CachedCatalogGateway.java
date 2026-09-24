package com.rpe.cardprocessor.client;

import com.rpe.cardprocessor.exception.CatalogUnavailableException;
import com.rpe.cardprocessor.service.CatalogGateway;
import com.rpe.cardprocessor.service.CatalogProduct;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * rpe_catalog products, cached in Redis ({@value #CACHE}, TTL from spring.cache.redis.time-to-live).
 * Only found products are cached: a 404 or a failure is asked again next time. A product changed in the catalog
 * is seen here once its cache entry expires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachedCatalogGateway implements CatalogGateway {

    public static final String CACHE = "catalog-products";

    private final CatalogClient client;

    @Override
    @Cacheable(cacheNames = CACHE, key = "#id", unless = "#result == null")
    public Optional<CatalogProduct> findProduct(UUID id) {
        log.debug("Fetching product id={} from rpe_catalog", id);
        try {
            var response = client.findProduct(id);
            return Optional.of(new CatalogProduct(response.id(), response.name(), response.description(), response.status()));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            throw new CatalogUnavailableException(ex);
        }
    }
}
