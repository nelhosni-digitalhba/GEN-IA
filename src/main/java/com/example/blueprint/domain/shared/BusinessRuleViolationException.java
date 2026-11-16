package com.example.blueprint.domain.shared;

/**
 * Levée quand une règle métier transverse est violée (ex : SKU déjà utilisé).
 * Traduite en HTTP 409 par la couche interfaces.
 */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
