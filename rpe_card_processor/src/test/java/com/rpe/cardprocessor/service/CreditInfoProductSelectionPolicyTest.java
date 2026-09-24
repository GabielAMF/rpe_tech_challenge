package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.TestCardProperties;
import com.rpe.cardprocessor.exception.CatalogUnavailableException;
import com.rpe.cardprocessor.exception.DefaultProductUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.rpe.cardprocessor.CardFixtures.GOLD;
import static com.rpe.cardprocessor.TestCardProperties.DEFAULT_PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditInfoProductSelectionPolicyTest {

    private static final UUID PLATINUM_ID = UUID.fromString("9e5a2d7c-1b3f-4c8a-a6d4-7b9e1f3a5c2d");
    private static final CatalogProduct PLATINUM = new CatalogProduct(PLATINUM_ID, "PLATINUM", null, "ATIVO");

    @Mock
    private CatalogGateway catalog;

    private CreditInfoProductSelectionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CreditInfoProductSelectionPolicy(catalog, TestCardProperties.properties());
    }

    @Test
    void usesActiveProductNamedByCreditInfo() {
        when(catalog.findProduct(PLATINUM_ID)).thenReturn(Optional.of(PLATINUM));

        assertThat(policy.select(" " + PLATINUM_ID + " ")).isEqualTo(PLATINUM);
        verify(catalog).findProduct(PLATINUM_ID);
        verifyNoMoreInteractions(catalog);
    }

    @Test
    void fallsBackToDefaultWhenNamedProductIsNotActive() {
        when(catalog.findProduct(PLATINUM_ID))
                .thenReturn(Optional.of(new CatalogProduct(PLATINUM_ID, "PLATINUM", null, "CANCELADO")));
        when(catalog.findProduct(DEFAULT_PRODUCT_ID)).thenReturn(Optional.of(GOLD));

        assertThat(policy.select(PLATINUM_ID.toString())).isEqualTo(GOLD);
    }

    @Test
    void fallsBackToDefaultWhenNamedProductDoesNotExist() {
        when(catalog.findProduct(PLATINUM_ID)).thenReturn(Optional.empty());
        when(catalog.findProduct(DEFAULT_PRODUCT_ID)).thenReturn(Optional.of(GOLD));

        assertThat(policy.select(PLATINUM_ID.toString())).isEqualTo(GOLD);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "score=780", "not-a-uuid"})
    void fallsBackToDefaultWhenCreditInfoIsNotAProductId(String creditInfo) {
        when(catalog.findProduct(DEFAULT_PRODUCT_ID)).thenReturn(Optional.of(GOLD));

        assertThat(policy.select(creditInfo)).isEqualTo(GOLD);
    }

    @Test
    void failsWhenDefaultProductIsMissing() {
        when(catalog.findProduct(DEFAULT_PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.select("score=780")).isInstanceOf(DefaultProductUnavailableException.class);
    }

    @Test
    void propagatesCatalogOutageSoTheMessageIsRetried() {
        when(catalog.findProduct(DEFAULT_PRODUCT_ID)).thenThrow(new CatalogUnavailableException(new RuntimeException()));

        assertThatThrownBy(() -> policy.select("score=780")).isInstanceOf(CatalogUnavailableException.class);
    }
}
