package com.rpe.catalog.domain;

import com.rpe.catalog.exception.ProductAlreadyActiveException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void newProductIsAtivoWithNormalizedName() {
        Product product = new Product(new ProductName(" gold "), "Gold card");

        assertThat(product.getName()).isEqualTo("GOLD");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void updateChangesNameAndDescriptionButNotStatus() {
        Product product = new Product(new ProductName("Gold"), "Gold card");
        product.cancel();

        product.update(new ProductName("Platinum"), "New");

        assertThat(product.getName()).isEqualTo("PLATINUM");
        assertThat(product.getDescription()).isEqualTo("New");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.CANCELADO);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new Product(null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void cancelIsIdempotent() {
        Product product = new Product(new ProductName("Gold"), null);
        product.cancel();

        product.cancel();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.CANCELADO);
    }

    @Test
    void activateTurnsCancelledProductActive() {
        Product product = new Product(new ProductName("Gold"), null);
        product.cancel();

        product.activate();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void activateRejectsActiveProduct() {
        Product product = new Product(new ProductName("Gold"), null);

        assertThatThrownBy(product::activate).isInstanceOf(ProductAlreadyActiveException.class);
    }
}
