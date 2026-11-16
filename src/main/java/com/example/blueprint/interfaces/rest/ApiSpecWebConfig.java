package com.example.blueprint.interfaces.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * API-FIRST : le contrat écrit à la main (resources/openapi/api-spec.yaml) est publié tel quel
 * sur /openapi/api-spec.yaml et affiché par Swagger UI (/swagger-ui.html).
 */
@Configuration
public class ApiSpecWebConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**").addResourceLocations("classpath:/openapi/");
    }
}
