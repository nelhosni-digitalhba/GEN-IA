package com.example.blueprint.infrastructure.ai;

import com.example.blueprint.application.port.ProductSemanticIndex;
import com.example.blueprint.domain.product.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * ADAPTATEUR RAG : indexe {@code Product.description} sous forme d'embeddings (nomic-embed-text via Ollama)
 * dans un vector store, et effectue la recherche par similarité.
 * <p>
 * Les appels d'embedding sont bloquants (HTTP) : on les déporte sur {@code boundedElastic}
 * pour ne jamais bloquer les threads Netty de WebFlux.
 */
@Component
public class VectorStoreProductIndex implements ProductSemanticIndex {

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    public VectorStoreProductIndex(VectorStore vectorStore,
                                   @Value("${blueprint.rag.similarity-threshold:0.3}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    @CircuitBreaker(name = "ollama")
    public Mono<Void> index(Product product) {
        return Mono.fromRunnable(() -> vectorStore.add(List.of(toDocument(product))))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> remove(String productId) {
        return Mono.fromRunnable(() -> vectorStore.delete(List.of(productId)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    @CircuitBreaker(name = "ollama")
    public Flux<ProductMatch> search(String need, int topK) {
        return Mono.fromCallable(() -> vectorStore.similaritySearch(SearchRequest.builder()
                        .query(need)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(docs -> docs)
                .map(VectorStoreProductIndex::toMatch);
    }

    static Document toDocument(Product p) {
        return new Document(p.id(), p.semanticText(), Map.of(
                "sku", p.sku().value(),
                "label", p.label(),
                "description", p.description()));
    }

    static ProductMatch toMatch(Document d) {
        Map<String, Object> m = d.getMetadata();
        return new ProductMatch(d.getId(), (String) m.get("sku"), (String) m.get("label"),
                (String) m.get("description"), d.getScore() == null ? 0.0 : d.getScore());
    }
}
