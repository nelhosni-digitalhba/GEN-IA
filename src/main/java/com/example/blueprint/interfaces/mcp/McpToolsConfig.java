package com.example.blueprint.interfaces.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enregistre les outils CRUD.
 * <p>
 * Le starter {@code spring-ai-starter-mcp-server-webflux} détecte automatiquement les beans
 * {@link ToolCallbackProvider} et les publie sur le serveur MCP (endpoint POST /mcp, protocole "stateless").
 * Le même bean est injecté dans l'assistant interne (OllamaAiAssistant).
 */
@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider crudTools(CustomerTools customerTools, ProductTools productTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(customerTools, productTools)
                .build();
    }
}
