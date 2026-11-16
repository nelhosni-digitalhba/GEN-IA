package com.example.blueprint.application.customer;

/** Données d'entrée d'un cas d'usage de création / modification de client. */
public record CustomerCommand(String nom, String prenom, String tel, String email, String adresse) {
}
