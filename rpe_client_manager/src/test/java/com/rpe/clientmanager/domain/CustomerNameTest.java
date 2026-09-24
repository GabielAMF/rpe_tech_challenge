package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.InvalidCustomerNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerNameTest {

    @Test
    void isTrimmed() {
        assertThat(new CustomerName("  Maria Silva ").value()).isEqualTo("Maria Silva");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlank(String value) {
        assertThatThrownBy(() -> new CustomerName(value)).isInstanceOf(InvalidCustomerNameException.class);
    }

    @Test
    void toStringHidesTheName() {
        assertThat(new CustomerName("Maria Silva").toString()).doesNotContain("Maria");
    }
}
