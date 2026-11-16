package com.example.blueprint.interfaces.rest.dto;

import com.example.blueprint.application.product.ProductCommand;

/** Corps de requête REST (schéma "ProductRequest" du contrat OpenAPI). */
public record ProductRequest(String sku, String label, String description) {

    public ProductCommand toCommand() {
        return new ProductCommand(sku, label, description);
    }
}
