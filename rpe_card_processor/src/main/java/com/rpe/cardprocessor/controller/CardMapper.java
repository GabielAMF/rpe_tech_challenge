package com.rpe.cardprocessor.controller;

import com.rpe.cardprocessor.controller.dto.CardResponse;
import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.domain.CardProduct;
import com.rpe.cardprocessor.service.CardDetails;
import org.springframework.stereotype.Component;

/** Converts cards into the API representation; exposes the masked number only. */
@Component
public class CardMapper {

    public CardResponse toResponse(CardDetails details) {
        Card card = details.card();
        CardProduct product = card.getProduct();
        return new CardResponse(
                card.getId(),
                card.getStatus(),
                card.getMaskedNumber(),
                new CardResponse.ProductResponse(product.id(), product.name(), product.description(), details.productStatus()),
                card.getCreatedAt());
    }
}
