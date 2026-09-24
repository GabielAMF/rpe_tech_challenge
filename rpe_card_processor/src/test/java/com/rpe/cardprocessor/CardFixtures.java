package com.rpe.cardprocessor;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.domain.CardOperator;
import com.rpe.cardprocessor.domain.CardProduct;
import com.rpe.cardprocessor.domain.GeneratedCardData;
import com.rpe.cardprocessor.service.CatalogProduct;

import java.time.YearMonth;
import java.util.UUID;

public final class CardFixtures {

    public static final GeneratedCardData DATA =
            new GeneratedCardData(CardOperator.MASTERCARD, "5112345678901234", YearMonth.of(2031, 9), "123");
    public static final CatalogProduct GOLD =
            new CatalogProduct(TestCardProperties.DEFAULT_PRODUCT_ID, "GOLD", "Standard card", "ATIVO");

    private CardFixtures() {
    }

    public static Card card(UUID customerId) {
        return new Card(UUID.randomUUID(), customerId, "Maria Silva", DATA,
                new CardProduct(GOLD.id(), GOLD.name(), GOLD.description()));
    }
}
