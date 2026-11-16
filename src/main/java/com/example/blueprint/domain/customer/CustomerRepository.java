package com.example.blueprint.domain.customer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * PORT de persistance (DDD / architecture hexagonale).
 * Le domaine définit le contrat ; l'infrastructure (MongoDB) fournit l'implémentation.
 * Seul Reactor (Mono/Flux) est utilisé : c'est un choix assumé pour un domaine réactif.
 */
public interface CustomerRepository {

    Mono<Customer> save(Customer customer);

    Mono<Customer> findById(String id);

    Flux<Customer> findAll();

    Mono<Boolean> existsByEmail(Email email);

    Mono<Void> deleteById(String id);
}
