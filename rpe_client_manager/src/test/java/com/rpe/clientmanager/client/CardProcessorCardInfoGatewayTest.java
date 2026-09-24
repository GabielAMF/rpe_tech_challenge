package com.rpe.clientmanager.client;

import com.rpe.clientmanager.service.CardLookup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The real Feign client against an in-process WireMock playing rpe_card_processor, including its failure modes.
 * Starts the full context, so it needs the infrastructure from docker-compose.yml.
 */
@SpringBootTest
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "integrations.card-processor.base-url"))
class CardProcessorCardInfoGatewayTest {

    private final UUID customerId = UUID.randomUUID();

    @Autowired
    private CardProcessorCardInfoGateway gateway;

    @Test
    void mapsCardAndProduct() {
        stubFor(get(cardUrl()).willReturn(okJson("""
                {"cardId": "0c6f8a2e-1b3d-4e5f-8a7b-9c0d1e2f3a4b", "status": "ATIVO",
                 "maskedNumber": "**** **** **** 1234", "createdAt": "2026-09-23T12:00:00Z",
                 "product": {"id": "3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b", "name": "GOLD",
                             "description": "Gold card", "status": "ATIVO"}}
                """)));

        CardLookup lookup = gateway.findByCustomerId(customerId);

        assertThat(lookup.available()).isTrue();
        assertThat(lookup.card().status()).isEqualTo("ATIVO");
        assertThat(lookup.card().maskedNumber()).isEqualTo("**** **** **** 1234");
        assertThat(lookup.card().product().name()).isEqualTo("GOLD");
    }

    @Test
    void notFoundMeansNoCardYet() {
        stubFor(get(cardUrl()).willReturn(aResponse().withStatus(404)));

        assertThat(gateway.findByCustomerId(customerId)).isEqualTo(CardLookup.notProducedYet());
    }

    @Test
    void serverErrorMeansUnavailable() {
        stubFor(get(cardUrl()).willReturn(aResponse().withStatus(500)));

        assertThat(gateway.findByCustomerId(customerId)).isEqualTo(CardLookup.unavailable());
    }

    @Test
    void slowResponseTimesOutAsUnavailable() {
        // read-timeout for card-processor is 2s in application.yml
        stubFor(get(cardUrl()).willReturn(okJson("{}").withFixedDelay(3000)));

        assertThat(gateway.findByCustomerId(customerId)).isEqualTo(CardLookup.unavailable());
    }

    private String cardUrl() {
        return "/api/v1/customers/" + customerId + "/card";
    }
}
