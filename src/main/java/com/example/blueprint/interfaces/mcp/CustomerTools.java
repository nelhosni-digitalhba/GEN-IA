package com.example.blueprint.interfaces.mcp;

import com.example.blueprint.application.customer.CustomerCommand;
import com.example.blueprint.application.customer.CustomerService;
import com.example.blueprint.interfaces.rest.dto.CustomerResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * OUTILS "Customer" décrits pour un LLM via {@code @Tool}.
 * <p>
 * Ces mêmes méthodes servent à DEUX usages :
 * <ul>
 *   <li>le serveur MCP (endpoint /mcp) : n'importe quel client MCP (Claude Desktop, IDE, agent) peut les appeler ;</li>
 *   <li>l'assistant interne (ChatClient + llama3.2) : function calling.</li>
 * </ul>
 * Les outils Spring AI sont synchrones : ils sont exécutés par Spring AI sur un thread "boundedElastic",
 * on peut donc attendre le résultat réactif avec un délai maximal.
 */
@Component
public class CustomerTools {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final CustomerService service;

    public CustomerTools(CustomerService service) {
        this.service = service;
    }

    @Tool(name = "createCustomer", description = "Crée un nouveau client. Tous les champs sont obligatoires.")
    public CustomerResponse createCustomer(
            @ToolParam(description = "Nom de famille, ex : CURIE") String nom,
            @ToolParam(description = "Prénom, ex : Marie") String prenom,
            @ToolParam(description = "Numéro de téléphone, ex : 0601020304") String tel,
            @ToolParam(description = "Adresse email, ex : marie@science.fr") String email,
            @ToolParam(description = "Adresse postale complète") String adresse) {
        return service.create(new CustomerCommand(nom, prenom, tel, email, adresse))
                .map(CustomerResponse::from).block(TIMEOUT);
    }

    @Tool(name = "listCustomers", description = "Liste tous les clients enregistrés.")
    public List<CustomerResponse> listCustomers() {
        return service.list().map(CustomerResponse::from).collectList().block(TIMEOUT);
    }

    @Tool(name = "getCustomer", description = "Récupère un client par son identifiant technique.")
    public CustomerResponse getCustomer(@ToolParam(description = "Identifiant du client") String id) {
        return service.get(id).map(CustomerResponse::from).block(TIMEOUT);
    }

    @Tool(name = "findCustomerByEmail", description = "Recherche un client par son adresse email.")
    public CustomerResponse findCustomerByEmail(@ToolParam(description = "Email du client") String email) {
        return service.findByEmail(email).map(CustomerResponse::from).block(TIMEOUT);
    }

    @Tool(name = "updateCustomer", description = "Met à jour toutes les coordonnées d'un client existant.")
    public CustomerResponse updateCustomer(
            @ToolParam(description = "Identifiant du client") String id,
            @ToolParam(description = "Nom de famille") String nom,
            @ToolParam(description = "Prénom") String prenom,
            @ToolParam(description = "Téléphone") String tel,
            @ToolParam(description = "Email") String email,
            @ToolParam(description = "Adresse postale") String adresse) {
        return service.update(id, new CustomerCommand(nom, prenom, tel, email, adresse))
                .map(CustomerResponse::from).block(TIMEOUT);
    }

    @Tool(name = "deleteCustomer", description = "Supprime un client par son identifiant.")
    public String deleteCustomer(@ToolParam(description = "Identifiant du client") String id) {
        service.delete(id).block(TIMEOUT);
        return "Client " + id + " supprimé";
    }
}
