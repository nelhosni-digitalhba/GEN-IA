package com.example.blueprint.application.assistant;

import com.example.blueprint.application.port.AiAssistantPort;
import com.example.blueprint.application.port.InputGuardrail;
import com.example.blueprint.application.port.ProductSemanticIndex.ProductMatch;
import com.example.blueprint.application.product.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ORCHESTRATEUR IA-FIRST (cas d'usage "parler à l'application en langage naturel").
 * <pre>
 *  1. Guardrail   : on refuse les messages dangereux / hors sujet AVANT d'appeler le LLM
 *  2. Retrieval   : on récupère les produits pertinents (RAG sur Product.description)
 *  3. Generation  : on délègue au LLM (qui peut aussi appeler les outils CRUD)
 * </pre>
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    static final int RAG_TOP_K = 4;

    private final InputGuardrail guardrail;
    private final ProductService productService;
    private final AiAssistantPort ai;

    public AssistantService(InputGuardrail guardrail, ProductService productService, AiAssistantPort ai) {
        this.guardrail = guardrail;
        this.productService = productService;
        this.ai = ai;
    }

    public Mono<AssistantAnswer> chat(String message) {
        InputGuardrail.Verdict verdict = guardrail.check(message);
        if (!verdict.allowed()) {
            return Mono.just(AssistantAnswer.blocked(verdict.reason()));
        }
        return retrieveContext(message)
                .flatMap(ctx -> ai.ask(message, ctx.text())
                        .map(answer -> AssistantAnswer.of(answer, ctx.skus())));
    }

    public Flux<String> stream(String message) {
        InputGuardrail.Verdict verdict = guardrail.check(message);
        if (!verdict.allowed()) {
            return Flux.just(AssistantAnswer.blocked(verdict.reason()).answer());
        }
        return retrieveContext(message).flatMapMany(ctx -> ai.stream(message, ctx.text()));
    }

    /** Étape "R" du RAG : produits candidats sous forme de texte injecté dans le prompt. */
    Mono<RagContext> retrieveContext(String message) {
        return productService.searchByNeed(message, RAG_TOP_K)
                .collectList()
                .onErrorResume(e -> {
                    log.warn("Recherche sémantique indisponible, on continue sans contexte : {}", e.getMessage());
                    return Mono.just(List.of());
                })
                .map(RagContext::from);
    }

    record RagContext(String text, List<String> skus) {
        static RagContext from(List<ProductMatch> matches) {
            if (matches.isEmpty()) {
                return new RagContext("(aucun produit pertinent trouvé dans le catalogue)", List.of());
            }
            String text = matches.stream()
                    .map(m -> "- SKU: %s | Libellé: %s | Description: %s".formatted(m.sku(), m.label(), m.description()))
                    .collect(Collectors.joining("\n"));
            return new RagContext(text, matches.stream().map(ProductMatch::sku).toList());
        }
    }
}
