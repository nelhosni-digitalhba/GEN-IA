package com.example.blueprint.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

interface SpringDataCustomerRepository extends ReactiveMongoRepository<CustomerDocument, String> {

    Mono<Boolean> existsByEmail(String email);
}
