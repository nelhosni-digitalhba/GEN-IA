package com.example.blueprint.domain.product;

import com.example.blueprint.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value Object "SKU" (Stock Keeping Unit) : référence produit unique.
 * Format : blocs alphanumériques en majuscules séparés par des tirets, ex : SKU-123, DESK-ERGO-01.
 */
public record Sku(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9]{2,10}(-[A-Z0-9]{1,10}){0,3}$");

    public Sku {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Le SKU est obligatoire");
        }
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainValidationException(
                    "SKU invalide : '" + value + "' (attendu : blocs alphanumériques séparés par '-', ex : SKU-123)");
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
