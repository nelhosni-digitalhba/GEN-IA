package com.example.blueprint.interfaces.mcp;

import com.example.blueprint.application.product.ProductCommand;
import com.example.blueprint.application.product.ProductService;
import com.example.blueprint.interfaces.rest.dto.ProductMatchResponse;
import com.example.blueprint.interfaces.rest.dto.ProductResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** OUTILS "Product" exposés via MCP et utilisés par l'assistant (function calling). */
@Component
public class ProductTools {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final ProductService service;

    public ProductTools(ProductService service) {
        this.service = service;
    }

    @Tool(name = "createProduct", description = "Crée un nouveau produit dans le catalogue.")
    public ProductResponse createProduct(
            @ToolParam(description = "Référence unique, blocs alphanumériques séparés par '-', ex : SKU-123") String sku,
            @ToolParam(description = "Libellé court du produit") String label,
            @ToolParam(description = "Description détaillée (au moins 10 caractères), utilisée pour la recherche sémantique") String description) {
        return service.create(new ProductCommand(sku, label, description))
                .map(ProductResponse::from).block(TIMEOUT);
    }

    @Tool(name = "listProducts", description = "Liste tous les produits du catalogue.")
    public List<ProductResponse> listProducts() {
        return service.list().map(ProductResponse::from).collectList().block(TIMEOUT);
    }

    @Tool(name = "searchProducts",
            description = "Recherche sémantique : trouve les produits adaptés à un besoin exprimé en langage naturel.")
    public List<ProductMatchResponse> searchProducts(
            @ToolParam(description = "Besoin du client, ex : 'courir sous la pluie'") String need) {
        return service.searchByNeed(need, 3).map(ProductMatchResponse::from).collectList().block(TIMEOUT);
    }

    @Tool(name = "updateProduct", description = "Met à jour le libellé et la description d'un produit (le SKU est immuable).")
    public ProductResponse updateProduct(
            @ToolParam(description = "Identifiant du produit") String id,
            @ToolParam(description = "Nouveau libellé") String label,
            @ToolParam(description = "Nouvelle description") String description) {
        return service.update(id, new ProductCommand(null, label, description))
                .map(ProductResponse::from).block(TIMEOUT);
    }

    @Tool(name = "deleteProduct", description = "Supprime un produit par son identifiant.")
    public String deleteProduct(@ToolParam(description = "Identifiant du produit") String id) {
        service.delete(id).block(TIMEOUT);
        return "Produit " + id + " supprimé";
    }
}
