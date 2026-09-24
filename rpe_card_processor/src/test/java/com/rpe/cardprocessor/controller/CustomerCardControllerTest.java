package com.rpe.cardprocessor.controller;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.exception.CardNotFoundException;
import com.rpe.cardprocessor.service.CardDetails;
import com.rpe.cardprocessor.service.CardQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static com.rpe.cardprocessor.CardFixtures.DATA;
import static com.rpe.cardprocessor.CardFixtures.GOLD;
import static com.rpe.cardprocessor.CardFixtures.card;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerCardController.class)
@Import(CardMapper.class)
class CustomerCardControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("7f1c2a4e-5b3d-4e8f-9a6b-1c2d3e4f5a6b");
    private static final UUID CARD_ID = UUID.fromString("0c6f8a2e-1b3d-4e5f-8a7b-9c0d1e2f3a4b");
    private static final Instant CREATED_AT = Instant.parse("2026-09-23T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardQueryService cardQueryService;

    @Test
    void returnsMaskedCardWithProduct() throws Exception {
        Card card = card(CUSTOMER_ID);
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        ReflectionTestUtils.setField(card, "createdAt", CREATED_AT);
        when(cardQueryService.findByCustomerId(CUSTOMER_ID)).thenReturn(new CardDetails(card, GOLD));

        mockMvc.perform(get("/api/v1/customers/{customerId}/card", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(CARD_ID.toString()))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.product.id").value(GOLD.id().toString()))
                .andExpect(jsonPath("$.product.name").value("GOLD"))
                .andExpect(jsonPath("$.product.status").value("ATIVO"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-23T12:00:00Z"))
                // Nothing sensitive leaves the service.
                .andExpect(content().string(not(containsString(DATA.number()))))
                .andExpect(content().string(not(containsString("Maria"))))
                .andExpect(jsonPath("$.cvv").doesNotExist())
                .andExpect(jsonPath("$.expiry").doesNotExist());
    }

    @Test
    void notFoundWhenCustomerHasNoCard() throws Exception {
        when(cardQueryService.findByCustomerId(CUSTOMER_ID)).thenThrow(new CardNotFoundException(CUSTOMER_ID));

        mockMvc.perform(get("/api/v1/customers/{customerId}/card", CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidCustomerIdUsesTheSameErrorShape() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{customerId}/card", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
