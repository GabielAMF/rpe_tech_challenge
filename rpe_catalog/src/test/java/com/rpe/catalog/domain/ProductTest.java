package com.rpe.catalog.domain;

import com.rpe.catalog.exception.InvalidProductNameException;
import com.rpe.catalog.exception.ProductAlreadyActiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void normalizeNameTrimsAndUpperCases() {
        assertThat(Product.normalizeName("  black card ")).isEqualTo("BLACK CARD");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void normalizeNameRejectsNameThatIsEmptyAfterTrim(String name) {
        assertThatThrownBy(() -> Product.normalizeName(name))
                .isInstanceOf(InvalidProductNameException.class);
    }

    @Test
    void constructorRejectsBlankName() {
        assertThatThrownBy(() -> new Product("   ", null))
                .isInstanceOf(InvalidProductNameException.class);
    }

    @Test
    void updateRejectsBlankNameAndKeepsOldValues() {
        Product product = new Product("Gold", "Gold card");

        assertThatThrownBy(() -> product.update(" ", "New"))
                .isInstanceOf(InvalidProductNameException.class);
        assertThat(product.getName()).isEqualTo("GOLD");
        assertThat(product.getDescription()).isEqualTo("Gold card");
    }

    @Test
    void activateTurnsCancelledProductActive() {
        Product product = new Product("Gold", null);
        product.cancel();

        product.activate();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ATIVO);
    }

    @Test
    void activateRejectsActiveProduct() {
        Product product = new Product("Gold", null);

        assertThatThrownBy(product::activate).isInstanceOf(ProductAlreadyActiveException.class);
    }
}
