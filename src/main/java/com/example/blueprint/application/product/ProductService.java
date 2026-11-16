package com.example.blueprint.application.product;

import com.example.blueprint.application.port.ProductSemanticIndex;
import com.example.blueprint.application.port.ProductSemanticIndex.ProductMatch;
import com.example.blueprint.domain.product.Product;
import com.example.blueprint.domain.product.ProductRepository;
import com.example.blueprint.domain.shared.BusinessRuleViolationException;
import com.example.blueprint.domain.shared.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage CRUD réactifs sur les produits + synchronisation de l'index sémantique (RAG).
 * <p>
 * Choix de conception : si l'indexation (Ollama) échoue, le CRUD réussit quand même
 * (la base MongoDB reste la source de vérité ; l'index est "best effort" et reconstruit au démarrage).
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;
    private final ProductSemanticIndex semanticIndex;

    public ProductService(ProductRepository repository, ProductSemanticIndex semanticIndex) {
        this.repository = repository;
        this.semanticIndex = semanticIndex;
    }

    public Mono<Product> create(ProductCommand cmd) {
        return Mono.fromCallable(() -> Product.create(cmd.sku(), cmd.label(), cmd.description()))
                .flatMap(product -> repository.existsBySku(product.sku())
                        .flatMap(exists -> exists
                                ? Mono.error(new BusinessRuleViolationException(
                                        "Le SKU " + product.sku() + " est déjà utilisé"))
                                : repository.save(product)))
                .flatMap(this::indexQuietly);
    }

    public Mono<Product> get(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)));
    }

    public Flux<Product> list() {
        return repository.findAll();
    }

    public Mono<Product> update(String id, ProductCommand cmd) {
        return get(id)
                .flatMap(existing -> {
                    if (cmd.sku() != null && !cmd.sku().isBlank()
                            && !existing.sku().value().equalsIgnoreCase(cmd.sku().strip())) {
                        return Mono.error(new BusinessRuleViolationException(
                                "Le SKU est l'identité métier du produit et ne peut pas être modifié"));
                    }
                    return repository.save(existing.update(cmd.label(), cmd.description()));
                })
                .flatMap(this::indexQuietly);
    }

    public Mono<Void> delete(String id) {
        return get(id)
                .flatMap(p -> repository.deleteById(p.id()))
                .then(semanticIndex.remove(id).onErrorResume(e -> {
                    log.warn("Suppression de l'index sémantique impossible pour {} : {}", id, e.getMessage());
                    return Mono.empty();
                }));
    }

    /** Recherche sémantique : "trouve-moi un produit pour ..." */
    public Flux<ProductMatch> searchByNeed(String need, int topK) {
        return semanticIndex.search(need, topK);
    }

    /** Reconstruit l'index vectoriel (en mémoire) à partir de MongoDB. */
    public Mono<Long> reindexAll() {
        return repository.findAll().concatMap(semanticIndex::index).count();
    }

    private Mono<Product> indexQuietly(Product product) {
        return semanticIndex.index(product)
                .onErrorResume(e -> {
                    log.warn("Produit {} enregistré mais non indexé (IA indisponible ?) : {}",
                            product.sku(), e.getMessage());
                    return Mono.empty();
                })
                .thenReturn(product);
    }
}
