package com.example.blueprint.interfaces.rest.dto;

import com.example.blueprint.domain.customer.Customer;

/** Représentation REST d'un client (schéma "Customer" du contrat OpenAPI). */
public record CustomerResponse(String id, String nom, String prenom, String tel, String email, String adresse) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.id(), c.nom(), c.prenom(), c.tel(), c.email().value(), c.adresse());
    }
}
