package com.rpe.cardprocessor.controller;

import com.rpe.cardprocessor.controller.dto.CardResponse;
import com.rpe.cardprocessor.service.CardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Called by rpe_client_manager. No authentication: internal-only for now (see README). */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerCardController {

    private final CardQueryService cardQueryService;
    private final CardMapper cardMapper;

    /** 404 while the card hasn't been produced yet. */
    @GetMapping("/{customerId}/card")
    public CardResponse findByCustomerId(@PathVariable UUID customerId) {
        return cardMapper.toResponse(cardQueryService.findByCustomerId(customerId));
    }
}
