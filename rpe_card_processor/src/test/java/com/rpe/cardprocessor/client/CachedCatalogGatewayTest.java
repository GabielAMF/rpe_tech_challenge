package com.rpe.cardprocessor.client;

import com.rpe.cardprocessor.exception.CatalogUnavailableException;
import com.rpe.cardprocessor.service.CatalogProduct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The real Feign client and Redis cache, with an in-process WireMock playing rpe_catalog.
 * Needs the infrastructure from docker-compose.yml (Redis, Postgres).
 */
@SpringBootTest
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "integrations.catalog.base-url"))
class CachedCatalogGatewayTest {

    private final UUID productId = UUID.randomUUID();

    @Autowired
    private CachedCatalogGateway gateway;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void evict() {
        cacheManager.getCache(CachedCatalogGateway.CACHE).evict(productId);
    }

    @Test
    void foundProductIsCachedInRedis() {
        stubFor(get(url()).willReturn(okJson("""
                {"id": "%s", "name": "PLATINUM", "description": "Platinum card", "status": "ATIVO",
                 "createdAt": "2026-09-23T12:00:00Z", "updatedAt": "2026-09-23T12:00:00Z"}
                """.formatted(productId))));

        Optional<CatalogProduct> first = gateway.findProduct(productId);
        Optional<CatalogProduct> second = gateway.findProduct(productId);

        assertThat(first).contains(new CatalogProduct(productId, "PLATINUM", "Platinum card", "ATIVO"));
        assertThat(second).isEqualTo(first);
        verify(1, getRequestedFor(urlEqualTo(url())));
    }

    @Test
    void unknownProductIsEmptyAndNotCached() {
        stubFor(get(url()).willReturn(aResponse().withStatus(404)));

        assertThat(gateway.findProduct(productId)).isEmpty();
        assertThat(gateway.findProduct(productId)).isEmpty();
        verify(2, getRequestedFor(urlEqualTo(url())));
    }

    @Test
    void catalogFailureIsReportedAndNotCached() {
        stubFor(get(url()).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> gateway.findProduct(productId)).isInstanceOf(CatalogUnavailableException.class);
        assertThat(cacheManager.getCache(CachedCatalogGateway.CACHE).get(productId)).isNull();
    }

    private String url() {
        return "/api/v1/products/" + productId;
    }
}
