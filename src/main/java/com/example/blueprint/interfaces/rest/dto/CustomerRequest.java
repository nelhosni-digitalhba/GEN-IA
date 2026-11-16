package com.example.blueprint.interfaces.rest.dto;

import com.example.blueprint.application.customer.CustomerCommand;

/** Corps de requête REST (schéma "CustomerRequest" du contrat OpenAPI). */
public record CustomerRequest(String nom, String prenom, String tel, String email, String adresse) {

    public CustomerCommand toCommand() {
        return new CustomerCommand(nom, prenom, tel, email, adresse);
    }
}
