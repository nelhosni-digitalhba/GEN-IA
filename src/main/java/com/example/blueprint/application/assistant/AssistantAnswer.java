package com.example.blueprint.application.assistant;

import java.util.List;

/**
 * Réponse de l'assistant.
 *
 * @param answer         texte généré (ou message de refus / de repli)
 * @param blocked        true si le guardrail a refusé la requête
 * @param candidateSkus  SKU des produits injectés dans le contexte RAG (traçabilité / anti-hallucination)
 */
public record AssistantAnswer(String answer, boolean blocked, List<String> candidateSkus) {

    public static AssistantAnswer of(String answer, List<String> skus) {
        return new AssistantAnswer(answer, false, skus);
    }

    public static AssistantAnswer blocked(String reason) {
        return new AssistantAnswer("⚠️ Requête refusée par le garde-fou : " + reason, true, List.of());
    }
}
