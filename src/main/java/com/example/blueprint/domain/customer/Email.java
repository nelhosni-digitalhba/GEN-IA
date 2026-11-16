package com.example.blueprint.domain.customer;

import com.example.blueprint.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value Object "Email" : immuable, toujours valide, normalisé en minuscules.
 * Deux emails sont égaux s'ils ont la même valeur (sémantique de record).
 */
public record Email(String value) {

    private static final Pattern FORMAT =
            Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("L'email est obligatoire");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("Email invalide : " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
