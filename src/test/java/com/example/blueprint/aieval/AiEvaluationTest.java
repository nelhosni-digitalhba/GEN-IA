package com.example.blueprint.aieval;

import com.example.blueprint.application.assistant.AssistantAnswer;
import com.example.blueprint.application.assistant.AssistantService;
import com.example.blueprint.application.product.ProductService;
import com.example.blueprint.infrastructure.ai.AiConfig;
import com.example.blueprint.infrastructure.ai.OllamaAiAssistant;
import com.example.blueprint.infrastructure.ai.SimplePromptGuardrail;
import com.example.blueprint.infrastructure.ai.VectorStoreProductIndex;
import com.example.blueprint.interfaces.mcp.ProductTools;
import com.example.blueprint.support.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ÉVALUATION IA (anti-hallucination) contre un VRAI Ollama (llama3.2 + nomic-embed-text).
 * <p>
 * Exclu de "mvn test" (tag ai-eval). Lancement : {@code mvn test -Pai-eval}
 * (Ollama doit tourner et les deux modèles être téléchargés). Exécuté dans un job dédié de la CI.
 */
@Tag("ai-eval")
class AiEvaluationTest {

    private static final Pattern SKU_LIKE = Pattern.compile("\\b[A-Z0-9]{2,10}(?:-[A-Z0-9]{1,10}){1,3}\\b");
    private static final Set<String> CATALOG = Set.of(
            "DESK-ERGO-01", "HEADSET-ANC-01", "SHOE-RUN-RAIN", "JACKET-RAIN-01");

    private static ProductService products;
    private static AssistantService assistant;

    @BeforeAll
    static void setUp() {
        String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
        OllamaChatModel chatModel = OllamaChatModel.builder().ollamaApi(api)
                .defaultOptions(OllamaChatOptions.builder().model("llama3.2").temperature(0.0).build())
                .build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder().ollamaApi(api)
                .defaultOptions(OllamaEmbeddingOptions.builder().model("nomic-embed-text").build())
                .build();

        var index = new VectorStoreProductIndex(SimpleVectorStore.builder(embeddingModel).build(), 0.3);
        products = new ProductService(new InMemoryProductRepository(), index);
        var tools = MethodToolCallbackProvider.builder().toolObjects(new ProductTools(products)).build();
        var llm = new OllamaAiAssistant(ChatClient.builder(chatModel).defaultSystem(AiConfig.SYSTEM_PROMPT).build(),
                tools, Duration.ofSeconds(120));
        assistant = new AssistantService(new SimplePromptGuardrail(), products, llm);

        create("DESK-ERGO-01", "Bureau assis-debout électrique",
                "Bureau réglable en hauteur, idéal pour le télétravail et pour soulager le dos.");
        create("HEADSET-ANC-01", "Casque à réduction de bruit",
                "Casque avec réduction active de bruit et micro, parfait pour les visioconférences.");
        create("SHOE-RUN-RAIN", "Chaussures de trail imperméables",
                "Chaussures de course avec membrane imperméable pour courir sous la pluie.");
        create("JACKET-RAIN-01", "Veste coupe-pluie",
                "Veste légère imperméable et respirante pour le running par temps humide.");
    }

    private static void create(String sku, String label, String description) {
        products.create(new com.example.blueprint.application.product.ProductCommand(sku, label, description)).block();
    }

    @Test
    void la_recherche_semantique_trouve_le_bon_produit() {
        var best = products.searchByNeed("je veux courir quand il pleut", 1).blockFirst();
        assertThat(best).isNotNull();
        assertThat(best.sku()).isIn("SHOE-RUN-RAIN", "JACKET-RAIN-01");
    }

    @Test
    void l_assistant_recommande_un_produit_existant() {
        AssistantAnswer answer = assistant.chat("Quel produit me conseilles-tu pour le télétravail ?").block();
        assertThat(answer).isNotNull();
        assertThat(answer.answer()).containsAnyOf("DESK-ERGO-01", "HEADSET-ANC-01");
        assertNoHallucinatedSku(answer.answer());
    }

    @Test
    void l_assistant_n_invente_pas_de_produit() {
        AssistantAnswer answer = assistant.chat("Vendez-vous un aspirateur robot ? Donne-moi son SKU.").block();
        assertThat(answer).isNotNull();
        assertNoHallucinatedSku(answer.answer());
    }

    /** Tout ce qui ressemble à un SKU dans la réponse DOIT exister dans le catalogue. */
    private static void assertNoHallucinatedSku(String text) {
        Matcher m = SKU_LIKE.matcher(text);
        while (m.find()) {
            assertThat(CATALOG).as("SKU halluciné dans : %s", text).contains(m.group());
        }
    }
}
