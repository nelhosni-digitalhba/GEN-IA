package com.example.blueprint.interfaces.rest;

import com.example.blueprint.application.customer.CustomerService;
import com.example.blueprint.interfaces.rest.dto.CustomerRequest;
import com.example.blueprint.interfaces.rest.dto.CustomerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

/** Implémentation WebFlux du contrat OpenAPI (tag "Customers") — voir resources/openapi/api-spec.yaml. */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<CustomerResponse> listCustomers() {
        return service.list().map(CustomerResponse::from);
    }

    @GetMapping("/{id}")
    public Mono<CustomerResponse> getCustomer(@PathVariable String id) {
        return service.get(id).map(CustomerResponse::from);
    }

    @PostMapping
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(@RequestBody CustomerRequest request) {
        return service.create(request.toCommand())
                .map(CustomerResponse::from)
                .map(c -> ResponseEntity.created(URI.create("/api/v1/customers/" + c.id())).body(c));
    }

    @PutMapping("/{id}")
    public Mono<CustomerResponse> updateCustomer(@PathVariable String id, @RequestBody CustomerRequest request) {
        return service.update(id, request.toCommand()).map(CustomerResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteCustomer(@PathVariable String id) {
        return service.delete(id);
    }
}
