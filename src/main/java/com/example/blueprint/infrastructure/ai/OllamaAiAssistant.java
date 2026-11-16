package com.example.blueprint.infrastructure.ai;

import com.example.blueprint.application.port.AiAssistantPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * ADAPTATEUR LLM : Spring AI {@link ChatClient} → Ollama (llama3.2).
 * <p>
 * Ce que fait chaque appel :
 * <ol>
 *   <li>prompt système (règles) + CONTEXTE PRODUITS (RAG) + message utilisateur ;</li>
 *   <li>mise à disposition des OUTILS CRUD (les mêmes que ceux exposés par le serveur MCP) :
 *       le LLM décide seul d'appeler {@code createCustomer}, {@code searchProducts}... ;</li>
 *   <li>protection Resilience4j : RateLimiter → CircuitBreaker → Retry → <b>fallback</b>.</li>
 * </ol>
 * Ordre des aspects Resilience4j (par défaut) : Retry( CircuitBreaker( RateLimiter( appel ) ) ).
 * Le fallback est donc porté par {@code @Retry}, la couche la plus externe.
 */
@Component
public class OllamaAiAssistant implements AiAssistantPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiAssistant.class);
    static final String RESILIENCE_INSTANCE = "ollama";

    private final ChatClient chatClient;
    private final ToolCallbackProvider crudTools;
    private final Duration streamIdleTimeout;

    public OllamaAiAssistant(ChatClient chatClient,
                             @Qualifier("crudTools") ToolCallbackProvider crudTools,
                             @Value("${blueprint.ai.stream-idle-timeout:60s}") Duration streamIdleTimeout) {
        this.chatClient = chatClient;
        this.crudTools = crudTools;
        this.streamIdleTimeout = streamIdleTimeout;
    }

    @Override
    @RateLimiter(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "askFallback")
    public Mono<String> ask(String userMessage, String productContext) {
        // ChatClient.call() est bloquant : on l'exécute hors des threads Netty.
        return Mono.fromCallable(() -> chatClient.prompt()
                        .system(withContext(productContext))
                        .user(userMessage)
                        .toolCallbacks(crudTools.getToolCallbacks())
                        .call()
                        .content())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @RateLimiter(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "streamFallback")
    public Flux<String> stream(String userMessage, String productContext) {
        return Flux.defer(() -> chatClient.prompt()
                        .system(withContext(productContext))
                        .user(userMessage)
                        .toolCallbacks(crudTools.getToolCallbacks())
                        .stream()
                        .content())
                // Si Llama "gèle" entre deux tokens, on coupe : l'erreur déclenche Retry puis le fallback.
                .timeout(streamIdleTimeout);
    }

    // ---------- Fallbacks Resilience4j (même signature + Throwable) ----------

    Mono<String> askFallback(String userMessage, String productContext, Throwable cause) {
        log.warn("Fallback IA (ask) : {}", cause.toString());
        return Mono.just(fallbackMessage(cause));
    }

    Flux<String> streamFallback(String userMessage, String productContext, Throwable cause) {
        log.warn("Fallback IA (stream) : {}", cause.toString());
        return Flux.just(fallbackMessage(cause));
    }

    static String fallbackMessage(Throwable cause) {
        return "🤖 L'assistant IA est momentanément indisponible (Ollama / Llama ne répond pas). "
                + "Les API CRUD classiques restent disponibles sur /api/v1/customers et /api/v1/products. "
                + "[" + cause.getClass().getSimpleName() + "]";
    }

    /**
     * Concaténation simple (pas de template) : les descriptions produit peuvent contenir des accolades
     * qui casseraient le moteur de template.
     */
    static String withContext(String productContext) {
        return AiConfig.SYSTEM_PROMPT + "\nCONTEXTE PRODUITS (issus de la recherche sémantique) :\n" + productContext;
    }
}
