package com.example.blueprint.application.port;

import com.example.blueprint.domain.product.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * PORT : index sémantique des produits (embeddings + vector store).
 * Implémenté dans infrastructure/ai par un SimpleVectorStore alimenté par Ollama (nomic-embed-text).
 */
public interface ProductSemanticIndex {

    Mono<Void> index(Product product);

    Mono<Void> remove(String productId);

    /** Recherche les produits dont la description est sémantiquement proche du besoin exprimé. */
    Flux<ProductMatch> search(String need, int topK);

    record ProductMatch(String productId, String sku, String label, String description, double score) {
    }
}
