package com.example.blueprint.domain.customer;

import com.example.blueprint.domain.shared.DomainValidationException;
import com.example.blueprint.domain.shared.Guard;

import java.util.regex.Pattern;

/**
 * Agrégat "Customer" (client).
 * <p>
 * Objet métier PUR : aucune annotation MongoDB, Spring ou IA.
 * Toutes les règles de validation vivent ici (et sont testées en TDD dans CustomerTest).
 *
 * @param id      identifiant technique (null tant que le client n'est pas persisté)
 * @param nom     nom de famille (obligatoire, normalisé en majuscules)
 * @param prenom  prénom (obligatoire)
 * @param tel     téléphone normalisé (chiffres, éventuellement préfixé par +)
 * @param email   Value Object Email
 * @param adresse adresse postale (obligatoire)
 */
public record Customer(String id, String nom, String prenom, String tel, Email email, String adresse) {

    /** Téléphone : 10 à 15 chiffres, "+" international autorisé en tête. */
    private static final Pattern TEL_FORMAT = Pattern.compile("^\\+?[0-9]{10,15}$");

    public Customer {
        nom = Guard.requireText(nom, "nom", 100).toUpperCase(java.util.Locale.ROOT);
        prenom = Guard.requireText(prenom, "prenom", 100);
        tel = normalizeTel(tel);
        if (email == null) {
            throw new DomainValidationException("L'email est obligatoire");
        }
        adresse = Guard.requireText(adresse, "adresse", 300);
    }

    /** Fabrique : création d'un nouveau client (pas encore d'identifiant). */
    public static Customer create(String nom, String prenom, String tel, String email, String adresse) {
        return new Customer(null, nom, prenom, tel, Email.of(email), adresse);
    }

    /** Comportement métier : modification des coordonnées, en conservant l'identité. */
    public Customer update(String nom, String prenom, String tel, String email, String adresse) {
        return new Customer(this.id, nom, prenom, tel, Email.of(email), adresse);
    }

    /** Retourne une copie portant l'identifiant attribué par la persistance. */
    public Customer withId(String newId) {
        return new Customer(newId, nom, prenom, tel, email, adresse);
    }

    public String nomComplet() {
        return prenom + " " + nom;
    }

    private static String normalizeTel(String raw) {
        String value = Guard.requireText(raw, "tel", 30).replaceAll("[\\s.\\-()]", "");
        if (!TEL_FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("Téléphone invalide : " + raw);
        }
        return value;
    }
}
