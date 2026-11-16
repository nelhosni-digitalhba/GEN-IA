package com.example.blueprint.domain.product;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** PORT de persistance des produits (implémenté par l'adaptateur MongoDB). */
public interface ProductRepository {

    Mono<Product> save(Product product);

    Mono<Product> findById(String id);

    Mono<Product> findBySku(Sku sku);

    Flux<Product> findAll();

    Mono<Boolean> existsBySku(Sku sku);

    Mono<Void> deleteById(String id);
}
