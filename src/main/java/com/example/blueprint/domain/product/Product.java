package com.example.blueprint.domain.product;

import com.example.blueprint.domain.shared.DomainValidationException;
import com.example.blueprint.domain.shared.Guard;

/**
 * Agrégat "Product".
 * <p>
 * La {@code description} est le texte qui sera vectorisé (embeddings) pour la recherche
 * sémantique (RAG) : une règle métier impose donc une description suffisamment riche.
 */
public record Product(String id, Sku sku, String label, String description) {

    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    public Product {
        if (sku == null) {
            throw new DomainValidationException("Le SKU est obligatoire");
        }
        label = Guard.requireText(label, "label", 150);
        description = Guard.requireText(description, "description", MAX_DESCRIPTION_LENGTH);
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            throw new DomainValidationException(
                    "La description doit contenir au moins " + MIN_DESCRIPTION_LENGTH
                            + " caractères (elle alimente la recherche sémantique)");
        }
    }

    public static Product create(String sku, String label, String description) {
        return new Product(null, Sku.of(sku), label, description);
    }

    /** Le SKU est l'identité métier : il n'est pas modifiable, seuls libellé et description évoluent. */
    public Product update(String label, String description) {
        return new Product(this.id, this.sku, label, description);
    }

    public Product withId(String newId) {
        return new Product(newId, sku, label, description);
    }

    /** Texte "indexable" utilisé pour le calcul des embeddings. */
    public String semanticText() {
        return label + "\n" + description;
    }
}
