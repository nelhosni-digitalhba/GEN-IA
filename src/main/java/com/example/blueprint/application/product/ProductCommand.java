package com.example.blueprint.application.product;

/** Données d'entrée d'un cas d'usage de création / modification de produit. */
public record ProductCommand(String sku, String label, String description) {
}
