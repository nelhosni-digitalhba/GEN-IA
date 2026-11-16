package com.example.blueprint.application;

import com.example.blueprint.application.port.ProductSemanticIndex;
import com.example.blueprint.application.product.ProductCommand;
import com.example.blueprint.application.product.ProductService;
import com.example.blueprint.domain.product.Product;
import com.example.blueprint.domain.shared.BusinessRuleViolationException;
import com.example.blueprint.support.InMemoryProductRepository;
import com.example.blueprint.support.KeywordProductIndex;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    private final InMemoryProductRepository repository = new InMemoryProductRepository();
    private final KeywordProductIndex index = new KeywordProductIndex();
    private final ProductService service = new ProductService(repository, index);

    private final ProductCommand lampe = new ProductCommand("SKU-123", "Lampe LED", "Lampe de bureau pour le télétravail");

    @Test
    void la_creation_indexe_le_produit_pour_la_recherche_semantique() {
        service.create(lampe).block();
        assertThat(index.size()).isEqualTo(1);
        StepVerifier.create(service.searchByNeed("matériel de télétravail", 3))
                .assertNext(m -> assertThat(m.sku()).isEqualTo("SKU-123"))
                .verifyComplete();
    }

    @Test
    void le_sku_est_unique() {
        service.create(lampe).block();
        StepVerifier.create(service.create(lampe)).expectError(BusinessRuleViolationException.class).verify();
    }

    @Test
    void le_sku_ne_peut_pas_etre_modifie() {
        Product p = service.create(lampe).block();
        StepVerifier.create(service.update(p.id(), new ProductCommand("SKU-999", "Lampe", "Nouvelle description")))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    @Test
    void la_suppression_retire_le_produit_de_l_index() {
        Product p = service.create(lampe).block();
        service.delete(p.id()).block();
        assertThat(index.size()).isZero();
    }

    @Test
    void le_crud_reussit_meme_si_l_index_ia_est_en_panne() {
        ProductSemanticIndex broken = new ProductSemanticIndex() {
            public Mono<Void> index(Product product) { return Mono.error(new IllegalStateException("Ollama down")); }
            public Mono<Void> remove(String id) { return Mono.error(new IllegalStateException("Ollama down")); }
            public Flux<ProductMatch> search(String need, int topK) { return Flux.error(new IllegalStateException()); }
        };
        ProductService resilient = new ProductService(repository, broken);
        StepVerifier.create(resilient.create(lampe))
                .assertNext(p -> assertThat(p.id()).isNotNull())
                .verifyComplete();
    }
}
