package com.example.blueprint.support;

import com.example.blueprint.application.port.AiAssistantPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Faux LLM déterministe : il "recommande" le premier produit du contexte RAG, ou avoue ne rien avoir.
 * Il permet de tester l'orchestration sans dépendre d'Ollama (non déterministe et lent).
 */
public class FakeAiAssistant implements AiAssistantPort {

    private String lastContext;
    private int calls;

    @Override
    public Mono<String> ask(String userMessage, String productContext) {
        calls++;
        lastContext = productContext;
        String firstSku = productContext.lines()
                .filter(l -> l.startsWith("- SKU: "))
                .map(l -> l.substring("- SKU: ".length(), l.indexOf(" |")))
                .findFirst().orElse(null);
        return Mono.just(firstSku == null
                ? "Je n'ai trouvé aucun produit correspondant dans le catalogue."
                : "Je vous conseille le produit " + firstSku + ".");
    }

    @Override
    public Flux<String> stream(String userMessage, String productContext) {
        return ask(userMessage, productContext).flatMapMany(a -> Flux.fromArray(a.split("(?<= )")));
    }

    public String lastContext() {
        return lastContext;
    }

    public int calls() {
        return calls;
    }
}
