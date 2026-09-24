package com.rpe.cardprocessor.service;

/**
 * Decides which catalog product a new card is issued for. CardProductionService depends on this interface, so the
 * real credit analysis can replace the current placeholder ({@link CreditInfoProductSelectionPolicy}) without
 * touching it.
 */
public interface ProductSelectionPolicy {

    /**
     * @return an ATIVO product
     * @throws com.rpe.cardprocessor.exception.DefaultProductUnavailableException if no usable product exists
     * @throws com.rpe.cardprocessor.exception.CatalogUnavailableException if rpe_catalog can't be reached
     */
    CatalogProduct select(String creditInfo);
}
