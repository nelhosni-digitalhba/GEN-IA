package com.example.blueprint.bdd;

import com.example.blueprint.application.assistant.AssistantAnswer;
import com.example.blueprint.application.assistant.AssistantService;
import com.example.blueprint.application.customer.CustomerCommand;
import com.example.blueprint.application.customer.CustomerService;
import com.example.blueprint.application.product.ProductCommand;
import com.example.blueprint.application.product.ProductService;
import com.example.blueprint.domain.customer.Customer;
import com.example.blueprint.infrastructure.ai.SimplePromptGuardrail;
import com.example.blueprint.support.FakeAiAssistant;
import com.example.blueprint.support.InMemoryCustomerRepository;
import com.example.blueprint.support.InMemoryProductRepository;
import com.example.blueprint.support.KeywordProductIndex;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Étantdonné;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Définitions des étapes Gherkin.
 * Les scénarios s'exécutent sur la VRAIE couche application (services, guardrail),
 * avec des adaptateurs en mémoire et un LLM simulé (déterministe) : rapides et reproductibles en CI.
 */
public class CatalogueSteps {

    private InMemoryProductRepository products;
    private KeywordProductIndex index;
    private ProductService productService;
    private CustomerService customerService;
    private FakeAiAssistant llm;
    private AssistantService assistant;

    private Throwable lastError;
    private AssistantAnswer lastAnswer;

    @Étantdonné("un catalogue vide")
    public void unCatalogueVide() {
        products = new InMemoryProductRepository();
        index = new KeywordProductIndex();
        productService = new ProductService(products, index);
        customerService = new CustomerService(new InMemoryCustomerRepository());
        llm = new FakeAiAssistant();
        assistant = new AssistantService(new SimplePromptGuardrail(), productService, llm);
    }

    @Étantdonné("le produit {string} {string} décrit par {string}")
    public void leProduit(String sku, String label, String description) {
        productService.create(new ProductCommand(sku, label, description)).block();
    }

    @Étantdonné("les produits suivants :")
    public void lesProduitsSuivants(List<Map<String, String>> rows) {
        rows.forEach(r -> leProduit(r.get("sku"), r.get("label"), r.get("description")));
    }

    @Quand("je crée le produit {string} {string} décrit par {string}")
    public void jeCreeLeProduit(String sku, String label, String description) {
        try {
            productService.create(new ProductCommand(sku, label, description)).block();
        } catch (RuntimeException e) {
            lastError = e;
        }
    }

    @Quand("je crée le client {string} {string} {string} {string} {string}")
    public void jeCreeLeClient(String nom, String prenom, String tel, String email, String adresse) {
        customerService.create(new CustomerCommand(nom, prenom, tel, email, adresse)).block();
    }

    @Quand("je demande à l'assistant {string}")
    public void jeDemandeALAssistant(String message) {
        lastAnswer = assistant.chat(message).block();
    }

    @Alors("le catalogue contient {int} produit(s)")
    public void leCatalogueContient(int count) {
        assertThat(productService.list().count().block()).isEqualTo(count);
    }

    @Et("le produit {string} est indexé pour la recherche sémantique")
    public void leProduitEstIndexe(String sku) {
        assertThat(productService.searchByNeed("télétravail", 5).map(m -> m.sku()).collectList().block())
                .contains(sku);
    }

    @Alors("la création est refusée avec le message {string}")
    public void laCreationEstRefusee(String fragment) {
        assertThat(lastError).isNotNull().hasMessageContaining(fragment);
    }

    @Alors("le client {string} existe avec le nom {string}")
    public void leClientExiste(String email, String nom) {
        Customer c = customerService.findByEmail(email).block();
        assertThat(c).isNotNull();
        assertThat(c.nom()).isEqualTo(nom);
    }

    @Alors("l'assistant recommande le produit {string}")
    public void lAssistantRecommande(String sku) {
        assertThat(lastAnswer.blocked()).isFalse();
        assertThat(lastAnswer.answer()).contains(sku);
    }

    @Et("l'assistant ne recommande pas le produit {string}")
    public void lAssistantNeRecommandePas(String sku) {
        assertThat(lastAnswer.answer()).doesNotContain(sku);
    }

    @Alors("l'assistant indique qu'aucun produit ne correspond")
    public void aucunProduit() {
        assertThat(lastAnswer.candidateSkus()).isEmpty();
        assertThat(lastAnswer.answer()).containsIgnoringCase("aucun produit");
    }

    @Alors("la requête est refusée par le garde-fou")
    public void refuseeParLeGardeFou() {
        assertThat(lastAnswer.blocked()).isTrue();
    }

    @Et("le modèle de langage n'a pas été appelé")
    public void llmNonAppele() {
        assertThat(llm.calls()).isZero();
    }
}
