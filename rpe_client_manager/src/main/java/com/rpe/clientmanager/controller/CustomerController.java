package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.controller.dto.CreateCustomerRequest;
import com.rpe.clientmanager.controller.dto.CustomerResponse;
import com.rpe.clientmanager.controller.dto.UpdateCustomerRequest;
import com.rpe.clientmanager.domain.Cpf;
import com.rpe.clientmanager.domain.Customer;
import com.rpe.clientmanager.service.CustomerService;
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

/** Any authenticated user (ADMIN or USER) can manage customers. */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id) {
        return customerMapper.toResponse(customerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer created = customerService.create(request.name(), new Cpf(request.cpf()), request.birthDate());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(customerMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        Customer updated = customerService.update(id, request.name(), request.birthDate(), request.status());
        return customerMapper.toResponse(updated);
    }

    @PostMapping("/{id}/activate")
    public CustomerResponse activate(@PathVariable UUID id) {
        return customerMapper.toResponse(customerService.activate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
