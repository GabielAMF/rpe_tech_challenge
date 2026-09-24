package com.rpe.cardprocessor;

import com.rpe.cardprocessor.config.CardProperties;

import java.util.UUID;

public final class TestCardProperties {

    public static final UUID DEFAULT_PRODUCT_ID = UUID.fromString("3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b");
    // base64 of 32 zero bytes: fine for tests only.
    public static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private TestCardProperties() {
    }

    public static CardProperties properties() {
        return new CardProperties(DEFAULT_PRODUCT_ID, KEY, 5);
    }
}
