package com.rpe.cardprocessor.exception;

import java.util.UUID;

/**
 * The configured default product doesn't exist in the catalog or isn't ATIVO: a configuration problem
 * (app.card.default-product-id), so no card can be produced for requests that fall back to it.
 */
public class DefaultProductUnavailableException extends CustomException {

    public DefaultProductUnavailableException(UUID productId) {
        super(ErrorCode.DEFAULT_PRODUCT_UNAVAILABLE,
                "Default product " + productId + " is missing or not ATIVO in rpe_catalog (check app.card.default-product-id)");
    }
}
