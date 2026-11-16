package com.example.blueprint.infrastructure.persistence;

import com.example.blueprint.domain.product.Product;
import com.example.blueprint.domain.product.ProductRepository;
import com.example.blueprint.domain.product.Sku;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** ADAPTATEUR : implémente le port du domaine avec Spring Data MongoDB réactif. */
@Repository
public class MongoProductRepository implements ProductRepository {

    private final SpringDataProductRepository delegate;

    public MongoProductRepository(SpringDataProductRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Product> save(Product product) {
        return delegate.save(toDocument(product)).map(MongoProductRepository::toDomain);
    }

    @Override
    public Mono<Product> findById(String id) {
        return delegate.findById(id).map(MongoProductRepository::toDomain);
    }

    @Override
    public Mono<Product> findBySku(Sku sku) {
        return delegate.findBySku(sku.value()).map(MongoProductRepository::toDomain);
    }

    @Override
    public Flux<Product> findAll() {
        return delegate.findAll().map(MongoProductRepository::toDomain);
    }

    @Override
    public Mono<Boolean> existsBySku(Sku sku) {
        return delegate.existsBySku(sku.value());
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return delegate.deleteById(id);
    }

    static ProductDocument toDocument(Product p) {
        return new ProductDocument(p.id(), p.sku().value(), p.label(), p.description());
    }

    static Product toDomain(ProductDocument d) {
        return new Product(d.id(), Sku.of(d.sku()), d.label(), d.description());
    }
}
