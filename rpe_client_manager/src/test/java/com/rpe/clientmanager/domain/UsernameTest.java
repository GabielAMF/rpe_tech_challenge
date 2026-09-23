package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.InvalidUsernameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameTest {

    @Test
    void trimsAndLowerCases() {
        assertThat(new Username("  Admin ").value()).isEqualTo("admin");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsNameThatIsEmptyAfterTrim(String value) {
        assertThatThrownBy(() -> new Username(value)).isInstanceOf(InvalidUsernameException.class);
    }
}
