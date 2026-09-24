package com.rpe.cardprocessor.client;

import com.rpe.cardprocessor.client.dto.CatalogProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Timeouts: spring.cloud.openfeign.client.config.catalog in application.yml. */
@FeignClient(name = "catalog", url = "${integrations.catalog.base-url}")
public interface CatalogClient {

    @GetMapping("/api/v1/products/{id}")
    CatalogProductResponse findProduct(@PathVariable("id") UUID id);
}
