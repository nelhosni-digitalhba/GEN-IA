package com.example.blueprint.interfaces.rest.dto;

import com.example.blueprint.application.port.ProductSemanticIndex.ProductMatch;

/** Résultat de recherche sémantique (schéma "ProductMatch" du contrat OpenAPI). */
public record ProductMatchResponse(String id, String sku, String label, String description, double score) {

    public static ProductMatchResponse from(ProductMatch m) {
        return new ProductMatchResponse(m.productId(), m.sku(), m.label(), m.description(), m.score());
    }
}
