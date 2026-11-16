package com.example.blueprint.domain.shared;

/** Petites gardes réutilisables pour exprimer les invariants du domaine. */
public final class Guard {

    private Guard() {
    }

    /** Vérifie qu'un texte est renseigné et ne dépasse pas une longueur maximale ; retourne la valeur nettoyée. */
    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Le champ '" + field + "' est obligatoire");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(
                    "Le champ '" + field + "' ne doit pas dépasser " + maxLength + " caractères");
        }
        return trimmed;
    }
}
