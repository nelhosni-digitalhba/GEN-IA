package com.example.blueprint.application;

import com.example.blueprint.application.customer.CustomerCommand;
import com.example.blueprint.application.customer.CustomerService;
import com.example.blueprint.domain.shared.BusinessRuleViolationException;
import com.example.blueprint.domain.shared.DomainValidationException;
import com.example.blueprint.domain.shared.ResourceNotFoundException;
import com.example.blueprint.support.InMemoryCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerServiceTest {

    private CustomerService service;
    private final CustomerCommand marie =
            new CustomerCommand("Curie", "Marie", "0601020304", "marie@science.fr", "1 rue de Paris");

    @BeforeEach
    void setUp() {
        service = new CustomerService(new InMemoryCustomerRepository());
    }

    @Test
    void cree_puis_relit_un_client() {
        var created = service.create(marie).block();
        assertThat(created.id()).isNotNull();
        StepVerifier.create(service.get(created.id()))
                .assertNext(c -> assertThat(c.nomComplet()).isEqualTo("Marie CURIE"))
                .verifyComplete();
    }

    @Test
    void refuse_deux_clients_avec_le_meme_email() {
        service.create(marie).block();
        StepVerifier.create(service.create(marie))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    @Test
    void propage_les_erreurs_de_validation_du_domaine() {
        StepVerifier.create(service.create(new CustomerCommand("Curie", "Marie", "12", "marie@science.fr", "Paris")))
                .expectError(DomainValidationException.class)
                .verify();
    }

    @Test
    void met_a_jour_et_supprime() {
        var created = service.create(marie).block();
        StepVerifier.create(service.update(created.id(),
                        new CustomerCommand("Curie", "Marie", "0700000000", "marie@sorbonne.fr", "Sorbonne")))
                .assertNext(c -> assertThat(c.email().value()).isEqualTo("marie@sorbonne.fr"))
                .verifyComplete();
        StepVerifier.create(service.delete(created.id())).verifyComplete();
        StepVerifier.create(service.get(created.id())).expectError(ResourceNotFoundException.class).verify();
    }
}
