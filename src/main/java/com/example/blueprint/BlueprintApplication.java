package com.example.blueprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée.
 * <pre>
 *  domain/          → cœur métier pur (Customer, Product, Email, Sku, ports)
 *  application/     → cas d'usage (CRUD + orchestrateur IA)
 *  infrastructure/  → MongoDB, Spring AI / Ollama, Resilience4j
 *  interfaces/      → REST WebFlux, MCP, UTCP
 * </pre>
 */
@SpringBootApplication
public class BlueprintApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlueprintApplication.class, args);
    }
}
