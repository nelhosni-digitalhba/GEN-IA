package com.example.blueprint.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rend la résilience OBSERVABLE : chaque transition du circuit breaker (CLOSED → OPEN → HALF_OPEN)
 * et chaque nouvelle tentative sont journalisées.
 * La configuration (seuils, durées) est dans application.yml, section {@code resilience4j}.
 * L'état est aussi visible via Actuator : GET /actuator/circuitbreakers et /actuator/health.
 */
@Component
public class ResilienceEventsLogger {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventsLogger.class);

    private final CircuitBreakerRegistry circuitBreakers;
    private final RetryRegistry retries;

    public ResilienceEventsLogger(CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
        this.circuitBreakers = circuitBreakers;
        this.retries = retries;
    }

    @PostConstruct
    void register() {
        circuitBreakers.circuitBreaker("ollama").getEventPublisher()
                .onStateTransition(e -> log.warn("⚡ CircuitBreaker '{}' : {}",
                        e.getCircuitBreakerName(), e.getStateTransition()));
        retries.retry("ollama").getEventPublisher()
                .onRetry(e -> log.info("🔁 Retry '{}' tentative #{} : {}",
                        e.getName(), e.getNumberOfRetryAttempts(), String.valueOf(e.getLastThrowable())));
    }
}
