package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.InvalidCpfException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test
    void removesFormatting() {
        assertThat(new Cpf(" 123.456.789-09 ").value()).isEqualTo("12345678909");
    }

    @Test
    void acceptsAlphanumericAndUpperCasesIt() {
        assertThat(new Cpf("ab1.cd2.ef3-45").value()).isEqualTo("AB1CD2EF345");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1234567890", "123456789012", "123.456.789/09", "12345678 9@"})
    void rejectsAnythingThatIsNot11LettersOrDigits(String value) {
        assertThatThrownBy(() -> new Cpf(value)).isInstanceOf(InvalidCpfException.class);
    }

    @Test
    void toStringIsMaskedSoItCantLeakIntoLogs() {
        Cpf cpf = new Cpf("12345678909");

        assertThat(cpf.masked()).isEqualTo("***.***.***-09");
        assertThat(cpf.toString()).isEqualTo("***.***.***-09").doesNotContain("123456");
    }

    @Test
    void invalidCpfMessageDoesNotEchoTheValue() {
        assertThatThrownBy(() -> new Cpf("98765"))
                .hasMessageNotContaining("98765");
    }
}
