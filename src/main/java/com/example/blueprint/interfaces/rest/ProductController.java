package com.example.blueprint.interfaces.rest;

import com.example.blueprint.application.product.ProductService;
import com.example.blueprint.interfaces.rest.dto.ProductMatchResponse;
import com.example.blueprint.interfaces.rest.dto.ProductRequest;
import com.example.blueprint.interfaces.rest.dto.ProductResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

/** Implémentation WebFlux du contrat OpenAPI (tag "Products"). */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<ProductResponse> listProducts() {
        return service.list().map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> getProduct(@PathVariable String id) {
        return service.get(id).map(ProductResponse::from);
    }

    @PostMapping
    public Mono<ResponseEntity<ProductResponse>> createProduct(@RequestBody ProductRequest request) {
        return service.create(request.toCommand())
                .map(ProductResponse::from)
                .map(p -> ResponseEntity.created(URI.create("/api/v1/products/" + p.id())).body(p));
    }

    @PutMapping("/{id}")
    public Mono<ProductResponse> updateProduct(@PathVariable String id, @RequestBody ProductRequest request) {
        return service.update(id, request.toCommand()).map(ProductResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(@PathVariable String id) {
        return service.delete(id);
    }

    /** Recherche sémantique (embeddings) : GET /api/v1/products/search?q=courir sous la pluie */
    @GetMapping("/search")
    public Flux<ProductMatchResponse> searchProducts(@RequestParam("q") String q,
                                                     @RequestParam(name = "topK", defaultValue = "3") int topK) {
        return service.searchByNeed(q, Math.max(1, Math.min(topK, 10))).map(ProductMatchResponse::from);
    }
}
