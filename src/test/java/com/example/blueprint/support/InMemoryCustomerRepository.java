package com.example.blueprint.support;

import com.example.blueprint.domain.customer.Customer;
import com.example.blueprint.domain.customer.CustomerRepository;
import com.example.blueprint.domain.customer.Email;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Doublure en mémoire du port CustomerRepository (tests rapides, sans MongoDB). */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Customer> save(Customer customer) {
        Customer saved = customer.id() == null ? customer.withId(UUID.randomUUID().toString()) : customer;
        store.put(saved.id(), saved);
        return Mono.just(saved);
    }

    @Override
    public Mono<Customer> findById(String id) {
        return Mono.justOrEmpty(store.get(id));
    }

    @Override
    public Flux<Customer> findAll() {
        return Flux.fromIterable(store.values());
    }

    @Override
    public Mono<Boolean> existsByEmail(Email email) {
        return Mono.just(store.values().stream().anyMatch(c -> c.email().equals(email)));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        store.remove(id);
        return Mono.empty();
    }
}
