package com.rpe.catalog.controller;

import com.rpe.catalog.controller.dto.CreateProductRequest;
import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.domain.ProductStatus;
import com.rpe.catalog.service.ProductService;
import com.rpe.catalog.service.exception.DuplicateProductNameException;
import com.rpe.catalog.service.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

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
                .andExpect(jsonPath("$.detail").value("Product " + ID + " not found"));
    }

    @Test
    void getReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-a-uuid"))
                .andExpect(status().isBadRequest());
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
    void putRejectsUnknownStatus() throws Exception {
        mockMvc.perform(put("/api/v1/products/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gold", "status": "SUSPENSO"}
                                """))
                .andExpect(status().isBadRequest());
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
