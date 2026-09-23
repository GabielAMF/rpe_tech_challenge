package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.config.ClockConfig;
import com.rpe.clientmanager.config.SecurityConfig;
import com.rpe.clientmanager.config.SecurityProblemHandler;
import com.rpe.clientmanager.domain.Cpf;
import com.rpe.clientmanager.domain.CustomerStatus;
import com.rpe.clientmanager.exception.CancelledCustomerExistsException;
import com.rpe.clientmanager.exception.CardProductionUnavailableException;
import com.rpe.clientmanager.exception.CustomerAlreadyActiveException;
import com.rpe.clientmanager.exception.CustomerNotFoundException;
import com.rpe.clientmanager.exception.StatusChangeNotAllowedException;
import com.rpe.clientmanager.service.CardInfo;
import com.rpe.clientmanager.service.CardLookup;
import com.rpe.clientmanager.service.CustomerDetails;
import com.rpe.clientmanager.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static com.rpe.clientmanager.CustomerFixtures.BIRTH_DATE;
import static com.rpe.clientmanager.CustomerFixtures.CPF;
import static com.rpe.clientmanager.CustomerFixtures.NOW;
import static com.rpe.clientmanager.CustomerFixtures.customer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({CustomerMapper.class, SecurityConfig.class, SecurityProblemHandler.class, ClockConfig.class})
class CustomerControllerTest {

    private static final UUID ID = UUID.fromString("7f1c2a4e-5b3d-4e8f-9a6b-1c2d3e4f5a6b");
    private static final UUID CARD_ID = UUID.fromString("0c6f8a2e-1b3d-4e5f-8a7b-9c0d1e2f3a4b");
    private static final UUID PRODUCT_ID = UUID.fromString("3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b");
    private static final RequestPostProcessor USER = jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    private static final String NEW_CUSTOMER = """
            {"name": "Maria Silva", "cpf": "123.456.789-09", "birthDate": "1990-05-20", "credit_info": "score=780"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(customerService);
    }

    @Test
    void getReturnsCustomerWithCardAndProduct() throws Exception {
        CardInfo card = new CardInfo(CARD_ID, "ISSUED", "**** **** **** 1234",
                new CardInfo.Product(PRODUCT_ID, "GOLD", "Gold card", "ATIVO"), NOW);
        when(customerService.getDetails(ID)).thenReturn(new CustomerDetails(customer(ID), CardLookup.found(card)));

        mockMvc.perform(get("/api/v1/customers/{id}", ID).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678909"))
                .andExpect(jsonPath("$.birthDate").value("1990-05-20"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.cardInfoAvailable").value(true))
                .andExpect(jsonPath("$.card.cardId").value(CARD_ID.toString()))
                .andExpect(jsonPath("$.card.maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.card.product.name").value("GOLD"));
    }

    @Test
    void getWithoutCardYetReturnsNullCard() throws Exception {
        when(customerService.getDetails(ID)).thenReturn(new CustomerDetails(customer(ID), CardLookup.notProducedYet()));

        mockMvc.perform(get("/api/v1/customers/{id}", ID).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card").value(nullValue()))
                .andExpect(jsonPath("$.cardInfoAvailable").value(true));
    }

    @Test
    void getStillReturnsCustomerWhenCardProcessorIsDown() throws Exception {
        when(customerService.getDetails(ID)).thenReturn(new CustomerDetails(customer(ID), CardLookup.unavailable()));

        mockMvc.perform(get("/api/v1/customers/{id}", ID).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.card").value(nullValue()))
                .andExpect(jsonPath("$.cardInfoAvailable").value(false));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(customerService.getDetails(ID)).thenThrow(new CustomerNotFoundException(ID));

        mockMvc.perform(get("/api/v1/customers/{id}", ID).with(USER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void postNormalizesCpfAndReturnsLocation() throws Exception {
        when(customerService.create("Maria Silva", new Cpf("12345678909"), BIRTH_DATE, "score=780")).thenReturn(customer(ID));

        mockMvc.perform(post("/api/v1/customers").with(USER)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/customers/" + ID))
                .andExpect(jsonPath("$.cpf").value("12345678909"));
    }

    @Test
    void postRejectsInvalidCpfWithoutEchoingIt() throws Exception {
        mockMvc.perform(post("/api/v1/customers").with(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Maria", "cpf": "98765", "birthDate": "1990-05-20", "creditInfo": "score=780"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CPF"))
                .andExpect(jsonPath("$.detail").value(not(containsString("98765"))));
    }

    @Test
    void postRejectsMissingFieldsAndFutureBirthDate() throws Exception {
        mockMvc.perform(post("/api/v1/customers").with(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "birthDate": "2999-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.cpf").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists())
                .andExpect(jsonPath("$.errors.creditInfo").exists());
    }

    @Test
    void postReturns503WhenCardProductionIsUnavailable() throws Exception {
        when(customerService.create(anyString(), any(Cpf.class), any(), anyString()))
                .thenThrow(new CardProductionUnavailableException(new RuntimeException("sqs down")));

        mockMvc.perform(post("/api/v1/customers").with(USER)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CARD_PRODUCTION_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(not(containsString("sqs down"))));
    }

    @Test
    void postWithCpfOfCancelledCustomerReturns409WithCustomerId() throws Exception {
        when(customerService.create(anyString(), any(Cpf.class), any(), anyString()))
                .thenThrow(new CancelledCustomerExistsException(ID, CPF));

        mockMvc.perform(post("/api/v1/customers").with(USER)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCELLED_CUSTOMER_EXISTS"))
                .andExpect(jsonPath("$.customerId").value(ID.toString()));
    }

    @Test
    void putPassesOptionalStatusAndIgnoresCpf() throws Exception {
        when(customerService.update(ID, "Maria Silva", BIRTH_DATE, CustomerStatus.BLOQUEADO)).thenReturn(customer(ID));

        mockMvc.perform(put("/api/v1/customers/{id}", ID).with(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Maria Silva", "cpf": "99999999999", "birthDate": "1990-05-20", "status": "BLOQUEADO"}
                                """))
                .andExpect(status().isOk());

        verify(customerService).update(ID, "Maria Silva", BIRTH_DATE, CustomerStatus.BLOQUEADO);
    }

    @Test
    void putWithDisallowedStatusReturns422() throws Exception {
        when(customerService.update(eq(ID), anyString(), any(), eq(CustomerStatus.ATIVO)))
                .thenThrow(new StatusChangeNotAllowedException(CustomerStatus.ATIVO));

        mockMvc.perform(put("/api/v1/customers/{id}", ID).with(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Maria Silva", "birthDate": "1990-05-20", "status": "ATIVO"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("STATUS_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void activateReturns422WhenAlreadyActive() throws Exception {
        when(customerService.activate(ID)).thenThrow(new CustomerAlreadyActiveException(ID));

        mockMvc.perform(post("/api/v1/customers/{id}/activate", ID).with(USER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CUSTOMER_ALREADY_ACTIVE"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/{id}", ID).with(USER))
                .andExpect(status().isNoContent());

        verify(customerService).cancel(ID);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new CustomerNotFoundException(ID)).when(customerService).cancel(ID);

        mockMvc.perform(delete("/api/v1/customers/{id}", ID).with(USER))
                .andExpect(status().isNotFound());
    }
}
