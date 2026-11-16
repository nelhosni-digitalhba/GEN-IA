package com.example.blueprint.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

interface SpringDataProductRepository extends ReactiveMongoRepository<ProductDocument, String> {

    Mono<ProductDocument> findBySku(String sku);

    Mono<Boolean> existsBySku(String sku);
}
