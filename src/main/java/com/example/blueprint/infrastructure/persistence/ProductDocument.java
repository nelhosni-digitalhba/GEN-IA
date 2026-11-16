package com.example.blueprint.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Représentation MongoDB d'un produit. */
@Document("products")
public record ProductDocument(
        @Id String id,
        @Indexed(unique = true) String sku,
        String label,
        String description) {
}
