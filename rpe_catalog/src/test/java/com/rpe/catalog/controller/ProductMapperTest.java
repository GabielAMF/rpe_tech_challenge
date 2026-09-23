package com.rpe.catalog.controller;

import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.domain.ProductStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.rpe.catalog.ProductFixtures.NOW;
import static com.rpe.catalog.ProductFixtures.product;
import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    @Test
    void mapsEveryField() {
        UUID id = UUID.randomUUID();

        ProductResponse response = new ProductMapper().toResponse(product(id, "Gold", "Gold card"));

        assertThat(response).isEqualTo(new ProductResponse(id, "GOLD", "Gold card", ProductStatus.ATIVO, NOW, NOW));
    }
}
