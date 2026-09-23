package com.rpe.catalog.controller;

import com.rpe.catalog.controller.dto.CreateProductRequest;
import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.controller.dto.UpdateProductRequest;
import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductName;
import com.rpe.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return productMapper.toResponse(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product created = productService.create(new ProductName(request.name()), request.description());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(productMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        Product updated = productService.update(id, new ProductName(request.name()), request.description());
        return productMapper.toResponse(updated);
    }

    @PostMapping("/{id}/activate")
    public ProductResponse activate(@PathVariable UUID id) {
        return productMapper.toResponse(productService.activate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
