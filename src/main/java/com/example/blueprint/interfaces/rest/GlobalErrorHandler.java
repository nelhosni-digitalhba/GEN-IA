package com.example.blueprint.interfaces.rest;

import com.example.blueprint.domain.shared.BusinessRuleViolationException;
import com.example.blueprint.domain.shared.DomainValidationException;
import com.example.blueprint.domain.shared.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/** Traduit les exceptions du domaine en réponses HTTP normalisées (RFC 9457 "Problem Details"). */
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail validation(DomainValidationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Donnée invalide", e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Ressource introuvable", e.getMessage());
    }

    @ExceptionHandler({BusinessRuleViolationException.class})
    public ProblemDetail conflict(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, "Règle métier violée", e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail duplicate(DuplicateKeyException e) {
        return problem(HttpStatus.CONFLICT, "Règle métier violée", "Doublon détecté (email ou SKU déjà utilisé)");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail circuitOpen(CallNotPermittedException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service IA indisponible",
                "Le circuit breaker est ouvert : Ollama est considéré comme indisponible, réessayez plus tard.");
    }

    /** Ollama injoignable (connexion refusée, délai dépassé) avant même l'ouverture du circuit. */
    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail aiUnreachable(ResourceAccessException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service IA indisponible",
                "Ollama est injoignable : vérifiez qu'il est démarré (ollama serve).");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
