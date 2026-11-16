package com.example.blueprint.application.customer;

import com.example.blueprint.domain.customer.Customer;
import com.example.blueprint.domain.customer.CustomerRepository;
import com.example.blueprint.domain.customer.Email;
import com.example.blueprint.domain.shared.BusinessRuleViolationException;
import com.example.blueprint.domain.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage CRUD réactifs sur les clients.
 * Utilisé à la fois par l'API REST, les outils MCP et l'assistant IA (une seule source de vérité).
 */
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Mono<Customer> create(CustomerCommand cmd) {
        return Mono.fromCallable(() -> Customer.create(cmd.nom(), cmd.prenom(), cmd.tel(), cmd.email(), cmd.adresse()))
                .flatMap(customer -> repository.existsByEmail(customer.email())
                        .flatMap(exists -> exists
                                ? Mono.error(new BusinessRuleViolationException(
                                        "Un client existe déjà avec l'email " + customer.email()))
                                : repository.save(customer)));
    }

    public Mono<Customer> get(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer", id)));
    }

    public Flux<Customer> list() {
        return repository.findAll();
    }

    public Mono<Customer> update(String id, CustomerCommand cmd) {
        return get(id).flatMap(existing -> {
            Customer updated = existing.update(cmd.nom(), cmd.prenom(), cmd.tel(), cmd.email(), cmd.adresse());
            Mono<Boolean> emailTaken = updated.email().equals(existing.email())
                    ? Mono.just(false)
                    : repository.existsByEmail(updated.email());
            return emailTaken.flatMap(taken -> taken
                    ? Mono.error(new BusinessRuleViolationException(
                            "Un client existe déjà avec l'email " + updated.email()))
                    : repository.save(updated));
        });
    }

    public Mono<Void> delete(String id) {
        return get(id).flatMap(c -> repository.deleteById(c.id()));
    }

    /** Petit utilitaire pour l'IA : recherche simple par email. */
    public Mono<Customer> findByEmail(String email) {
        Email target = Email.of(email);
        return repository.findAll().filter(c -> c.email().equals(target)).next();
    }
}
