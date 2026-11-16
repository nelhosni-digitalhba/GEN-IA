package com.example.blueprint.interfaces.rest;

import com.example.blueprint.application.assistant.AssistantAnswer;
import com.example.blueprint.application.assistant.AssistantService;
import com.example.blueprint.interfaces.rest.dto.AssistantRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Endpoints IA-First (tag "Assistant" du contrat OpenAPI). */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    /** Réponse complète en JSON (pratique pour les tests et les agents). */
    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AssistantAnswer> chat(@RequestBody AssistantRequest request) {
        return assistant.chat(request.message());
    }

    /** Réponse streamée en temps réel (Server-Sent Events) : chaque événement = un fragment de texte. */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody AssistantRequest request) {
        return assistant.stream(request.message());
    }
}
