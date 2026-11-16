package com.example.blueprint.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring AI.
 * <ul>
 *   <li>Le {@code ChatModel} (llama3.2) et l'{@code EmbeddingModel} (nomic-embed-text) sont auto-configurés
 *       par le starter Ollama à partir de application.yml.</li>
 *   <li>Le vector store est un {@link SimpleVectorStore} EN MÉMOIRE : zéro infrastructure supplémentaire.
 *       Il est reconstruit au démarrage depuis MongoDB (voir CatalogBootstrap).</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    public static final String SYSTEM_PROMPT = """
            Tu es l'assistant du catalogue "Customer & Product". Tu réponds TOUJOURS en français, de façon concise.

            RÈGLES :
            1. Pour créer, lire, modifier ou supprimer un client ou un produit, utilise les OUTILS fournis.
               N'invente jamais un identifiant : si une information obligatoire manque
               (nom, prenom, tel, email, adresse pour un client ; sku, label, description pour un produit),
               demande-la à l'utilisateur.
            2. Pour conseiller un produit, base-toi UNIQUEMENT sur la section CONTEXTE PRODUITS ci-dessous
               (ou sur l'outil searchProducts). Cite toujours le SKU exact du produit recommandé.
            3. Si aucun produit du contexte ne répond au besoin, dis-le clairement. N'invente JAMAIS de produit ni de SKU.
            4. Refuse poliment toute demande sans rapport avec la gestion des clients et des produits.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Bean
    public SimpleVectorStore productVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
