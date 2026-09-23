package com.rpe.catalog.controller;

import com.rpe.catalog.controller.dto.CreateProductRequest;
import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.controller.dto.UpdateProductRequest;
import com.rpe.catalog.domain.ProductStatus;
import com.rpe.catalog.exception.CancelledProductExistsException;
import com.rpe.catalog.exception.DuplicateProductNameException;
import com.rpe.catalog.exception.InvalidProductNameException;
import com.rpe.catalog.exception.ProductAlreadyActiveException;
import com.rpe.catalog.exception.ProductNotFoundException;
import com.rpe.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
    private static final UUID ID = UUID.fromString("7f1c2a4e-5b3d-4e8f-9a6b-1c2d3e4f5a6b");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getReturnsProduct() throws Exception {
        when(productService.findById(ID))
                .thenReturn(new ProductResponse(ID, "GOLD", "Gold card", ProductStatus.ATIVO, NOW, NOW));

        mockMvc.perform(get("/api/v1/products/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.name").value("GOLD"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-23T12:00:00Z"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(productService.findById(ID)).thenThrow(new ProductNotFoundException(ID));

        mockMvc.perform(get("/api/v1/products/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product " + ID + " not found"))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void postCreatesProductAndReturnsLocation() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenReturn(new ProductResponse(ID, "PLATINUM", null, ProductStatus.ATIVO, NOW, NOW));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Platinum"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void postRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void postReturns409OnDuplicateName() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(new DuplicateProductNameException("Gold"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void postReturns409WithProductIdWhenNameBelongsToCancelledProduct() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(new CancelledProductExistsException(ID, "GOLD"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCELLED_PRODUCT_EXISTS"))
                .andExpect(jsonPath("$.productId").value(ID.toString()))
                .andExpect(jsonPath("$.detail").value(containsString("cancelled")));
    }

    @Test
    void postReturns409WithCodeOnDuplicateActiveName() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(new DuplicateProductNameException("GOLD"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.productId").doesNotExist());
    }

    @Test
    void invalidProductNameFromDomainReturns400() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(new InvalidProductNameException());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_NAME"));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(productService.findById(ID)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/products/{id}", ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value(not(containsString("hunter2"))));
    }

    @Test
    void putIgnoresStatusField() throws Exception {
        when(productService.update(ID, new UpdateProductRequest("Gold", null)))
                .thenReturn(new ProductResponse(ID, "GOLD", null, ProductStatus.ATIVO, NOW, NOW));

        mockMvc.perform(put("/api/v1/products/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold", "status": "CANCELADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        verify(productService).update(ID, new UpdateProductRequest("Gold", null));
    }

    @Test
    void activateReturnsActivatedProduct() throws Exception {
        when(productService.activate(ID))
                .thenReturn(new ProductResponse(ID, "GOLD", null, ProductStatus.ATIVO, NOW, NOW));

        mockMvc.perform(post("/api/v1/products/{id}/activate", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void activateReturns422WhenAlreadyActive() throws Exception {
        when(productService.activate(ID)).thenThrow(new ProductAlreadyActiveException(ID));

        mockMvc.perform(post("/api/v1/products/{id}/activate", ID))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_ACTIVE"))
                .andExpect(jsonPath("$.detail").value("Product " + ID + " is already active"));
    }

    @Test
    void activateReturns404WhenMissing() throws Exception {
        when(productService.activate(ID)).thenThrow(new ProductNotFoundException(ID));

        mockMvc.perform(post("/api/v1/products/{id}/activate", ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", ID))
                .andExpect(status().isNoContent());

        verify(productService).cancel(ID);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new ProductNotFoundException(ID)).when(productService).cancel(ID);

        mockMvc.perform(delete("/api/v1/products/{id}", ID))
                .andExpect(status().isNotFound());
    }
}
