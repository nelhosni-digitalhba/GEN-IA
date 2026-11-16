package com.example.blueprint.domain.shared;

/** Levée quand un agrégat demandé n'existe pas. Traduite en HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String type, String id) {
        super(type + " introuvable : " + id);
    }
}
