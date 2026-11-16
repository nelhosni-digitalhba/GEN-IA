package com.example.blueprint.application.port;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * PORT : accès au modèle de langage (LLM).
 * L'implémentation (infrastructure/ai) branche Spring AI + Ollama, les outils CRUD et Resilience4j.
 */
public interface AiAssistantPort {

    /** Réponse complète (non streamée). */
    Mono<String> ask(String userMessage, String productContext);

    /** Réponse streamée token par token. */
    Flux<String> stream(String userMessage, String productContext);
}
