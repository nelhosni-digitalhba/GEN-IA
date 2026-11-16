package com.example.blueprint.support;

import com.example.blueprint.application.port.ProductSemanticIndex;
import com.example.blueprint.domain.product.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Doublure déterministe de l'index sémantique : simple recouvrement de mots-clés.
 * Elle remplace les embeddings (Ollama) dans les tests unitaires et BDD.
 */
public class KeywordProductIndex implements ProductSemanticIndex {

    private final Map<String, Product> indexed = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> index(Product product) {
        indexed.put(product.id(), product);
        return Mono.empty();
    }

    @Override
    public Mono<Void> remove(String productId) {
        indexed.remove(productId);
        return Mono.empty();
    }

    @Override
    public Flux<ProductMatch> search(String need, int topK) {
        Set<String> query = words(need);
        return Flux.fromIterable(indexed.values())
                .map(p -> {
                    Set<String> doc = words(p.semanticText());
                    long common = query.stream().filter(doc::contains).count();
                    double score = query.isEmpty() ? 0 : (double) common / query.size();
                    return new ProductMatch(p.id(), p.sku().value(), p.label(), p.description(), score);
                })
                .filter(m -> m.score() > 0)
                .sort(Comparator.comparingDouble(ProductMatch::score).reversed())
                .take(topK);
    }

    public int size() {
        return indexed.size();
    }

    private static Set<String> words(String text) {
        String n = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(n.split("[^a-z0-9]+")).filter(w -> w.length() > 3).collect(Collectors.toSet());
    }
}
