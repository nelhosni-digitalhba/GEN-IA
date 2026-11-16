package com.example.blueprint.domain.shared;

/**
 * Levée quand une règle d'invariant du domaine est violée (donnée invalide).
 * Traduite en HTTP 400 par la couche interfaces.
 */
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) {
        super(message);
    }
}
