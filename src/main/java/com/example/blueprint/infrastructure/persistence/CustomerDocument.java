package com.example.blueprint.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Représentation MongoDB d'un client (séparée de l'agrégat du domaine). */
@Document("customers")
public record CustomerDocument(
        @Id String id,
        String nom,
        String prenom,
        String tel,
        @Indexed(unique = true) String email,
        String adresse) {
}
