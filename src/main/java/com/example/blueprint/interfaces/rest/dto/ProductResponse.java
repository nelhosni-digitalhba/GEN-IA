package com.example.blueprint.interfaces.rest.dto;

import com.example.blueprint.domain.product.Product;

/** Représentation REST d'un produit (schéma "Product" du contrat OpenAPI). */
public record ProductResponse(String id, String sku, String label, String description) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.sku().value(), p.label(), p.description());
    }
}
