package com.example.blueprint.interfaces.utcp;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UTCP (Universal Tool Calling Protocol) — le "Manual".
 * <p>
 * Différence clé avec MCP :
 * <ul>
 *   <li><b>MCP</b> : l'agent parle à un SERVEUR intermédiaire (JSON-RPC sur /mcp) qui appelle le code.</li>
 *   <li><b>UTCP</b> : l'agent lit ce manuel puis appelle DIRECTEMENT les endpoints REST natifs
 *       (/api/v1/...), avec leur protocole d'origine. Aucun wrapper, aucune couche en plus.</li>
 * </ul>
 * Format : UTCP 1.0 (tools[] + tool_call_template de type "http").
 */
@RestController
public class UtcpManualController {

    static final String UTCP_VERSION = "1.0.1";
    static final String MANUAL_VERSION = "1.0.0";

    @GetMapping(value = "/.well-known/utcp.json", produces = "application/json")
    public Map<String, Object> manual(ServerHttpRequest request) {
        String base = UriComponentsBuilder.fromUri(request.getURI())
                .replacePath(null).replaceQuery(null).fragment(null).build().toUriString();
        return manual(base);
    }

    /** Construit le manuel pour une URL de base donnée (méthode pure, testée unitairement). */
    public static Map<String, Object> manual(String baseUrl) {
        String customers = baseUrl + "/api/v1/customers";
        String products = baseUrl + "/api/v1/products";
        Map<String, Object> id = Map.of("id", Map.of("type", "string", "description", "Identifiant technique"));

        List<Map<String, Object>> tools = List.of(
                tool("list_customers", "Liste tous les clients", List.of("customer", "crud"),
                        schema(Map.of(), List.of()), arrayOf(customerSchema()), http(customers, "GET", false)),
                tool("get_customer", "Récupère un client par identifiant", List.of("customer", "crud"),
                        schema(id, List.of("id")), customerSchema(), http(customers + "/{id}", "GET", false)),
                tool("create_customer", "Crée un client (nom, prenom, tel, email, adresse)", List.of("customer", "crud"),
                        schema(Map.of("body", customerRequestSchema()), List.of("body")), customerSchema(),
                        http(customers, "POST", true)),
                tool("update_customer", "Met à jour un client", List.of("customer", "crud"),
                        schema(merge(id, Map.of("body", customerRequestSchema())), List.of("id", "body")),
                        customerSchema(), http(customers + "/{id}", "PUT", true)),
                tool("delete_customer", "Supprime un client", List.of("customer", "crud"),
                        schema(id, List.of("id")), Map.of(), http(customers + "/{id}", "DELETE", false)),
                tool("list_products", "Liste tous les produits", List.of("product", "crud"),
                        schema(Map.of(), List.of()), arrayOf(productSchema()), http(products, "GET", false)),
                tool("get_product", "Récupère un produit par identifiant", List.of("product", "crud"),
                        schema(id, List.of("id")), productSchema(), http(products + "/{id}", "GET", false)),
                tool("create_product", "Crée un produit (sku, label, description)", List.of("product", "crud"),
                        schema(Map.of("body", productRequestSchema()), List.of("body")), productSchema(),
                        http(products, "POST", true)),
                tool("update_product", "Met à jour le libellé et la description d'un produit", List.of("product", "crud"),
                        schema(merge(id, Map.of("body", productRequestSchema())), List.of("id", "body")),
                        productSchema(), http(products + "/{id}", "PUT", true)),
                tool("delete_product", "Supprime un produit", List.of("product", "crud"),
                        schema(id, List.of("id")), Map.of(), http(products + "/{id}", "DELETE", false)),
                tool("search_products", "Recherche sémantique de produits selon un besoin en langage naturel",
                        List.of("product", "ai", "rag"),
                        schema(Map.of(
                                "q", Map.of("type", "string", "description", "Besoin, ex : courir sous la pluie"),
                                "topK", Map.of("type", "integer", "default", 3)), List.of("q")),
                        arrayOf(Map.of("type", "object")), http(products + "/search", "GET", false)),
                tool("ask_assistant", "Pose une question en langage naturel à l'assistant IA du catalogue",
                        List.of("ai", "assistant"),
                        schema(Map.of("body", schema(Map.of("message", Map.of("type", "string")), List.of("message"))),
                                List.of("body")),
                        Map.of("type", "object"), http(baseUrl + "/api/v1/assistant/chat", "POST", true)));

        Map<String, Object> manual = new LinkedHashMap<>();
        manual.put("utcp_version", UTCP_VERSION);
        manual.put("manual_version", MANUAL_VERSION);
        manual.put("tools", tools);
        return manual;
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> tool(String name, String description, List<String> tags,
                                            Map<String, Object> inputs, Map<String, Object> outputs,
                                            Map<String, Object> callTemplate) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("description", description);
        t.put("tags", tags);
        t.put("inputs", inputs);
        t.put("outputs", outputs);
        t.put("tool_call_template", callTemplate);
        return t;
    }

    /** Variables {xxx} de l'URL = paramètres de chemin ; "body" = corps JSON ; le reste = query string. */
    private static Map<String, Object> http(String url, String method, boolean hasBody) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("name", "customer_product_api");
        c.put("call_template_type", "http");
        c.put("url", url);
        c.put("http_method", method);
        c.put("content_type", "application/json");
        if (hasBody) {
            c.put("body_field", "body");
        }
        return c;
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        s.put("required", required);
        return s;
    }

    private static Map<String, Object> arrayOf(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> m = new LinkedHashMap<>(a);
        m.putAll(b);
        return m;
    }

    private static Map<String, Object> customerRequestSchema() {
        Map<String, Object> str = Map.of("type", "string");
        return schema(Map.of("nom", str, "prenom", str, "tel", str,
                "email", Map.of("type", "string", "format", "email"), "adresse", str),
                List.of("nom", "prenom", "tel", "email", "adresse"));
    }

    private static Map<String, Object> customerSchema() {
        Map<String, Object> s = customerRequestSchema();
        @SuppressWarnings("unchecked")
        Map<String, Object> props = new LinkedHashMap<>((Map<String, Object>) s.get("properties"));
        props.put("id", Map.of("type", "string"));
        return schema(props, List.of("id"));
    }

    private static Map<String, Object> productRequestSchema() {
        return schema(Map.of(
                        "sku", Map.of("type", "string", "pattern", "^[A-Z0-9]{2,10}(-[A-Z0-9]{1,10}){0,3}$"),
                        "label", Map.of("type", "string"),
                        "description", Map.of("type", "string", "minLength", 10)),
                List.of("sku", "label", "description"));
    }

    private static Map<String, Object> productSchema() {
        Map<String, Object> s = productRequestSchema();
        @SuppressWarnings("unchecked")
        Map<String, Object> props = new LinkedHashMap<>((Map<String, Object>) s.get("properties"));
        props.put("id", Map.of("type", "string"));
        return schema(props, List.of("id"));
    }
}
