package com.rpe.clientmanager.client;

import com.rpe.clientmanager.client.dto.CardProcessorCardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Timeouts: spring.cloud.openfeign.client.config.card-processor in application.yml. */
@FeignClient(name = "card-processor", url = "${integrations.card-processor.base-url}")
public interface CardProcessorClient {

    /** 404 while the card hasn't been produced yet. */
    @GetMapping("/api/v1/customers/{customerId}/card")
    CardProcessorCardResponse findCardByCustomerId(@PathVariable("customerId") UUID customerId);
}
