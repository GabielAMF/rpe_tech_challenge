package com.rpe.catalog.domain;

import com.rpe.catalog.exception.InvalidProductNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductNameTest {

    @Test
    void trimsAndUpperCases() {
        assertThat(new ProductName("  black card ").value()).isEqualTo("BLACK CARD");
    }

    @Test
    void namesThatNormalizeTheSameAreEqual() {
        assertThat(new ProductName(" gold ")).isEqualTo(new ProductName("GOLD"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejectsNameThatIsEmptyAfterTrim(String name) {
        assertThatThrownBy(() -> new ProductName(name))
                .isInstanceOf(InvalidProductNameException.class);
    }
}
