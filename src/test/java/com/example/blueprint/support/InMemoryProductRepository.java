package com.example.blueprint.support;

import com.example.blueprint.domain.product.Product;
import com.example.blueprint.domain.product.ProductRepository;
import com.example.blueprint.domain.product.Sku;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Doublure en mémoire du port ProductRepository. */
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Product> save(Product product) {
        Product saved = product.id() == null ? product.withId(UUID.randomUUID().toString()) : product;
        store.put(saved.id(), saved);
        return Mono.just(saved);
    }

    @Override
    public Mono<Product> findById(String id) {
        return Mono.justOrEmpty(store.get(id));
    }

    @Override
    public Mono<Product> findBySku(Sku sku) {
        return Flux.fromIterable(store.values()).filter(p -> p.sku().equals(sku)).next();
    }

    @Override
    public Flux<Product> findAll() {
        return Flux.fromIterable(store.values());
    }

    @Override
    public Mono<Boolean> existsBySku(Sku sku) {
        return findBySku(sku).hasElement();
    }

    @Override
    public Mono<Void> deleteById(String id) {
        store.remove(id);
        return Mono.empty();
    }
}
