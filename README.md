# Customer & Product AI Blueprint

Projet pédagogique **Java 21 / Spring Boot 3.5 réactif** qui réunit sur un domaine simple
(**Customer** : nom, prénom, tel, email, adresse — **Product** : sku, label, description) :

**DDD · API-First · IA-First · WebFlux · MongoDB · Spring AI + Ollama (Llama 3.2) · RAG ·
Resilience4j · MCP · UTCP · TDD · BDD (Cucumber) · CI/CD (GitHub Actions + Docker)**

| Document | Contenu |
|---|---|
| [docs/README.md](docs/README.md) | Guide pas-à-pas : lancer, tester avec `curl`, couper Ollama… |
| [docs/BLUEPRINT.md](docs/BLUEPRINT.md) | Architecture en Mermaid, MCP vs UTCP, résilience, tests |
| [api-spec.yaml](src/main/resources/openapi/api-spec.yaml) | Contrat OpenAPI (API-First) |

Démarrage express :

```bash
docker-compose up -d
```

```bash
ollama pull llama3.2 && ollama pull nomic-embed-text
```

```bash
mvn spring-boot:run
```
