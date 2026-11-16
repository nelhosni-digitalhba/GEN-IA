package com.example.blueprint.infrastructure.persistence;

import com.example.blueprint.domain.customer.Customer;
import com.example.blueprint.domain.customer.CustomerRepository;
import com.example.blueprint.domain.customer.Email;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** ADAPTATEUR : implémente le port du domaine avec Spring Data MongoDB réactif. */
@Repository
public class MongoCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository delegate;

    public MongoCustomerRepository(SpringDataCustomerRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Customer> save(Customer customer) {
        return delegate.save(toDocument(customer)).map(MongoCustomerRepository::toDomain);
    }

    @Override
    public Mono<Customer> findById(String id) {
        return delegate.findById(id).map(MongoCustomerRepository::toDomain);
    }

    @Override
    public Flux<Customer> findAll() {
        return delegate.findAll().map(MongoCustomerRepository::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(Email email) {
        return delegate.existsByEmail(email.value());
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return delegate.deleteById(id);
    }

    static CustomerDocument toDocument(Customer c) {
        return new CustomerDocument(c.id(), c.nom(), c.prenom(), c.tel(), c.email().value(), c.adresse());
    }

    static Customer toDomain(CustomerDocument d) {
        return new Customer(d.id(), d.nom(), d.prenom(), d.tel(), Email.of(d.email()), d.adresse());
    }
}
