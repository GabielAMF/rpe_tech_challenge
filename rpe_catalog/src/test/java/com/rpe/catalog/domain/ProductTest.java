package com.rpe.catalog.domain;

import com.rpe.catalog.exception.InvalidProductNameException;
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

        assertThatThrownBy(() -> product.update(" ", "New", ProductStatus.CANCELADO))
                .isInstanceOf(InvalidProductNameException.class);
        assertThat(product.getName()).isEqualTo("GOLD");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ATIVO);
    }
}
