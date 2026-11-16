# 🧭 Blueprint d'architecture — Customer & Product AI

> Objectif : montrer, sur un domaine volontairement banal (des **clients** et des **produits**),
> comment s'articulent concrètement **DDD, API-First, IA-First, WebFlux, MongoDB, Spring AI / Llama,
> Resilience4j, MCP, UTCP, TDD, BDD et CI/CD**.

---

## 1. Vue d'ensemble

```mermaid
flowchart TB
    subgraph Clients["👥 Qui appelle l'application ?"]
        H["Humain / Front<br/>(curl, Swagger UI)"]
        A1["Agent IA compatible MCP<br/>(Claude Desktop, IDE...)"]
        A2["Agent IA compatible UTCP"]
    end

    subgraph Interfaces["🚪 interfaces/ — portes d'entrée"]
        REST["REST WebFlux<br/>/api/v1/customers<br/>/api/v1/products"]
        CHAT["Assistant IA<br/>/api/v1/assistant/chat<br/>/api/v1/assistant/stream (SSE)"]
        MCP["Serveur MCP<br/>POST /mcp (JSON-RPC)<br/>outils @Tool"]
        UTCP["Manual UTCP<br/>GET /.well-known/utcp.json"]
    end

    subgraph Application["⚙️ application/ — cas d'usage"]
        CS["CustomerService"]
        PS["ProductService"]
        AS["AssistantService<br/>(Guardrail → RAG → LLM)"]
    end

    subgraph Domain["💎 domain/ — cœur métier PUR"]
        C["Customer<br/>+ Email (VO)"]
        P["Product<br/>+ Sku (VO)"]
        PORTS["Ports :<br/>CustomerRepository<br/>ProductRepository"]
    end

    subgraph Infra["🔌 infrastructure/ — adaptateurs techniques"]
        MONGO_AD["persistence/<br/>Mongo*Repository"]
        AI_AD["ai/<br/>OllamaAiAssistant<br/>VectorStoreProductIndex<br/>SimplePromptGuardrail"]
        R4J["resilience/<br/>CircuitBreaker · Retry · RateLimiter"]
    end

    subgraph Ext["🖥️ Services externes (100 % locaux)"]
        DB[("MongoDB")]
        OLL["Ollama<br/>llama3.2 · nomic-embed-text"]
        VS[("SimpleVectorStore<br/>(en mémoire)")]
    end

    H --> REST & CHAT
    A1 --> MCP
    A2 -. "lit le manuel" .-> UTCP
    A2 == "appelle DIRECTEMENT" ==> REST

    REST --> CS & PS
    CHAT --> AS
    MCP --> CS & PS
    AS --> PS
    AS --> AI_AD
    CS & PS --> PORTS
    PORTS -. "implémenté par" .-> MONGO_AD
    PS --> AI_AD
    MONGO_AD --> DB
    AI_AD --> R4J --> OLL
    AI_AD --> VS
    AI_AD -. "function calling" .-> MCP
```

**La règle d'or (DDD / hexagonal)** : les flèches de dépendance pointent **vers le domaine**.
Le package `domain` n'importe ni Spring, ni MongoDB, ni Spring AI : seulement Reactor (`Mono`/`Flux`),
un choix assumé pour un domaine réactif.

---

## 2. Les couches DDD, concrètement

| Couche | Package | Contient | Dépend de |
|---|---|---|---|
| **Domaine** | `domain/` | `Customer`, `Product` (agrégats), `Email`, `Sku` (Value Objects), ports `*Repository`, exceptions métier | rien (Java + Reactor) |
| **Application** | `application/` | `CustomerService`, `ProductService`, `AssistantService`, ports `AiAssistantPort`, `ProductSemanticIndex`, `InputGuardrail` | domaine |
| **Infrastructure** | `infrastructure/` | Documents + repositories MongoDB, Spring AI (ChatClient, VectorStore), guardrail, Resilience4j, données de démo | application, domaine, frameworks |
| **Interfaces** | `interfaces/` | Contrôleurs REST, outils MCP `@Tool`, Manual UTCP, gestion d'erreurs | application |

### Où vivent les règles métier ?

| Règle | Où | Test |
|---|---|---|
| Email valide et normalisé en minuscules | `Email` (VO) | `EmailTest` |
| SKU au format `ABC-123`, en majuscules | `Sku` (VO) | `SkuTest` |
| Nom/prénom/adresse obligatoires, téléphone 10–15 chiffres | `Customer` | `CustomerTest` |
| Description ≥ 10 caractères (elle alimente le RAG) | `Product` | `ProductTest` |
| Le SKU est l'identité métier → **non modifiable** | `Product.update()` + `ProductService` | `ProductTest`, `ProductServiceTest` |
| Email et SKU **uniques** | `CustomerService`, `ProductService` (+ index unique MongoDB) | `*ServiceTest`, BDD |

---

## 3. API-First

```mermaid
flowchart LR
    SPEC["📄 api-spec.yaml<br/>(écrit EN PREMIER)"] --> CTRL["Contrôleurs WebFlux"]
    SPEC --> SWAGGER["Swagger UI<br/>/swagger-ui.html"]
    SPEC --> UTCPM["Manual UTCP<br/>(même routes)"]
    CTRL -. "vérifié par" .-> TEST["ApiContractTest<br/>❌ échoue si code ≠ contrat"]
```

1. On écrit `src/main/resources/openapi/api-spec.yaml` (routes, schémas, codes d'erreur RFC 9457).
2. Les contrôleurs implémentent exactement ces opérations.
3. `ApiContractTest` compare **automatiquement** les opérations du contrat et celles du code :
   une route oubliée ou non documentée casse le build.

---

## 4. IA-First : le parcours d'une question

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant C as AssistantController<br/>(WebFlux)
    participant S as AssistantService
    participant G as Guardrail
    participant V as VectorStore<br/>(nomic-embed-text)
    participant R as Resilience4j
    participant L as Llama 3.2<br/>(Ollama)
    participant T as Outils @Tool
    participant M as MongoDB

    U->>C: POST /api/v1/assistant/stream<br/>"Ajoute le client Marie Curie..."
    C->>S: stream(message)
    S->>G: check(message)
    alt injection / suppression massive
        G-->>U: ⚠️ refus (le LLM n'est jamais appelé)
    end
    S->>V: similaritySearch(message, top 4)
    V-->>S: produits proches (contexte RAG)
    S->>R: ai.stream(message, contexte)
    R->>L: prompt système + CONTEXTE PRODUITS + message + outils
    L-->>R: tool_call createCustomer(nom, prenom, tel, email, adresse)
    R->>T: CustomerTools.createCustomer(...)
    T->>M: insert (après validation du domaine)
    M-->>T: Customer{id}
    T-->>L: résultat JSON
    L-->>U: Flux<String> : "C'est fait ! Marie CURIE a été créée..." (token par token)
```

**Deux mécanismes complémentaires :**

| Mécanisme | Quand | Comment |
|---|---|---|
| **RAG** (Retrieval-Augmented Generation) | « Quel produit pour le télétravail ? » | `Product.description` est vectorisée (nomic-embed-text) dans un `SimpleVectorStore`. Les 4 produits les plus proches sont injectés dans le prompt. |
| **Function calling** | « Ajoute le client… », « Liste les produits » | Le LLM reçoit la liste des outils `@Tool` et décide lui-même d'appeler `createCustomer`, `searchProducts`… |

**Anti-hallucination :** prompt système strict (« n'invente jamais de SKU »), température basse (0.2),
`candidateSkus` renvoyés dans la réponse (traçabilité), et test `AiEvaluationTest` qui vérifie que
**tout SKU cité par le LLM existe réellement**.

---

## 5. Résilience (Resilience4j)

```mermaid
flowchart LR
    IN["Appel IA"] --> RT["🔁 Retry<br/>2 tentatives<br/>500 ms"]
    RT --> CB["⚡ CircuitBreaker<br/>50 % d'échecs<br/>sur 10 appels"]
    CB --> RL["🚦 RateLimiter<br/>10 req/s"]
    RL --> OL["Ollama"]
    RT -. "échec final" .-> FB["🛟 Fallback<br/>message de repli"]
```

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN: ≥ 50 % d'échecs (min. 4 appels)
    OPEN --> HALF_OPEN: après 20 s
    HALF_OPEN --> CLOSED: 2 appels test OK
    HALF_OPEN --> OPEN: appel test KO
    note right of OPEN
        Plus aucun appel vers Ollama :
        réponse immédiate via le fallback
        (ou HTTP 503 sur /products/search)
    end note
```

Points d'attention (et pourquoi) :

* **Une seule couche de retry.** Spring AI possède son propre `RetryTemplate` (10 tentatives,
  backoff jusqu'à 3 min !). Il est désactivé (`spring.ai.retry.max-attempts: 1`) : sinon une panne
  d'Ollama bloquerait chaque requête pendant de longues minutes.
* **Le fallback est porté par `@Retry`**, la couche la plus externe : si on le mettait sur
  `@CircuitBreaker`, le Retry ne verrait jamais d'erreur et ne réessaierait jamais.
* **Le CRUD ne dépend pas de l'IA.** Si l'indexation (embeddings) échoue, le produit est quand même
  enregistré dans MongoDB ; l'index est reconstruit au prochain démarrage.
* **Observabilité** : `GET /actuator/circuitbreakers`, `/actuator/health`, et logs `⚡`/`🔁`.

---

## 6. MCP vs UTCP, appliqués à ce projet

```mermaid
sequenceDiagram
    autonumber
    participant AM as Agent MCP
    participant MS as Serveur MCP<br/>(/mcp, dans l'app)
    participant SV as CustomerService
    participant AU as Agent UTCP
    participant API as API REST native<br/>(/api/v1/...)

    rect rgb(235, 245, 255)
    Note over AM,SV: MCP — l'agent passe par un SERVEUR intermédiaire
    AM->>MS: JSON-RPC tools/list
    MS-->>AM: createCustomer, searchProducts...
    AM->>MS: JSON-RPC tools/call createCustomer{...}
    MS->>SV: createCustomer(...)
    SV-->>MS: Customer
    MS-->>AM: result.content[0].text
    end

    rect rgb(240, 255, 240)
    Note over AU,API: UTCP — l'agent lit un MANUEL puis appelle l'API d'origine
    AU->>API: GET /.well-known/utcp.json
    API-->>AU: tools[] + tool_call_template (url, méthode, body)
    AU->>API: POST /api/v1/customers {...} (appel HTTP natif)
    API-->>AU: 201 Created
    end
```

| Critère | **MCP** (`POST /mcp`) | **UTCP** (`GET /.well-known/utcp.json`) |
|---|---|---|
| Philosophie | « Un protocole et un serveur dédiés aux agents » | « Un manuel qui décrit comment appeler l'existant » |
| Transport | JSON-RPC 2.0 (ici : HTTP *stateless*) | Le protocole natif de l'outil (ici : HTTP/REST) |
| Code à écrire | Classes `@Tool` (`CustomerTools`, `ProductTools`) | Un document JSON (`UtcpManualController`) |
| Chemin d'un appel | Agent → serveur MCP → service | Agent → API REST → service (**un saut de moins**) |
| Sécurité / auth | Gérées par le serveur MCP | Celles de l'API existante (rien à dupliquer) |
| Écosystème | Très large (Claude Desktop, IDE, frameworks d'agents) | Plus jeune, en croissance |
| Idéal pour | Outils « pensés pour l'IA », logique d'orchestration côté serveur | Exposer à des agents une API qui existe déjà, sans wrapper |

**Dans ce projet, les deux coexistent sans duplication de logique métier** : outils MCP, API REST et
assistant interne appellent tous les mêmes services applicatifs.

---

## 7. Stratégie de tests

```mermaid
flowchart TB
    subgraph Rapide["Exécuté à chaque `mvn test` (aucun service externe)"]
        TDD["🧪 TDD — JUnit 5<br/>domaine, services, guardrail, UTCP"]
        CONTRACT["📄 ApiContractTest<br/>code ↔ api-spec.yaml"]
        BDD["🥒 BDD — Cucumber (français)<br/>scénarios métier + LLM simulé"]
        IT["🔗 ApplicationIntegrationTest<br/>WebFlux + MCP + UTCP + fallback"]
    end
    subgraph Lent["mvn test -Pai-eval (Ollama réel)"]
        EVAL["🤖 AiEvaluationTest<br/>pertinence RAG + 0 SKU halluciné"]
    end
```

| Type | Fichiers | Ce qu'on prouve |
|---|---|---|
| TDD | `domain/*Test`, `application/*Test` | Les règles métier, sans framework, en millisecondes |
| BDD | `features/customer_product_ai.feature` | Le comportement attendu, lisible par un non-développeur |
| Contrat | `ApiContractTest` | API-First tenu dans la durée |
| Intégration | `ApplicationIntegrationTest` | Câblage Spring, sérialisation, MCP, UTCP, fallback |
| Évaluation IA | `AiEvaluationTest` (tag `ai-eval`) | Le vrai Llama recommande des produits existants, n'en invente pas |

---

## 8. CI/CD

```mermaid
flowchart LR
    PUSH["git push / PR"] --> B["build-and-test<br/>mvn verify<br/>(TDD + BDD + intégration)"]
    B --> E["ai-evaluation<br/>Ollama + llama3.2<br/>mvn test -Pai-eval<br/>(main / manuel)"]
    B --> D["docker<br/>build multi-stage<br/>push GHCR (main)"]
```

Fichier : `.github/workflows/ci-cd.yml`. Image : `Dockerfile` multi-stage (JDK + Maven pour construire,
JRE seul + couches Spring Boot extraites pour exécuter, utilisateur non-root).

---

## 9. Choix techniques et limites assumées

| Choix | Pourquoi | Pour aller en production |
|---|---|---|
| `SimpleVectorStore` en mémoire | Zéro infra en plus | PgVector, MongoDB Atlas Vector Search, Qdrant… |
| Reactor dans le domaine | Pragmatisme dans une app 100 % réactive | Ports purement synchrones si le domaine doit être réutilisé ailleurs |
| Outils `@Tool` synchrones (`block()` borné) | API Spring AI ; exécutés sur `boundedElastic` | Outils réactifs quand l'API le permettra |
| Guardrail à base de regex | Lisible et testable | Modèle de modération + garde-fous de sortie |
| Pas d'authentification | Lisibilité de la démo | OAuth2 / JWT sur REST, MCP et UTCP (champ `auth` du manuel) |
| Pas de mémoire de conversation | Simplicité | `MessageChatMemoryAdvisor` + `conversationId` |
