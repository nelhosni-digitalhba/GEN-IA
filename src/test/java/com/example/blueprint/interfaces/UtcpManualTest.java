package com.example.blueprint.interfaces;

import com.example.blueprint.interfaces.utcp.UtcpManualController;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UtcpManualTest {

    @Test
    @SuppressWarnings("unchecked")
    void le_manual_decrit_des_appels_http_directs_vers_l_api_native() {
        Map<String, Object> manual = UtcpManualController.manual("http://localhost:8080");

        assertThat(manual).containsKeys("utcp_version", "manual_version", "tools");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) manual.get("tools");
        assertThat(tools).extracting(t -> t.get("name")).contains(
                "create_customer", "list_customers", "create_product", "search_products", "ask_assistant");

        Map<String, Object> createCustomer = tools.stream()
                .filter(t -> t.get("name").equals("create_customer")).findFirst().orElseThrow();
        Map<String, Object> template = (Map<String, Object>) createCustomer.get("tool_call_template");
        assertThat(template)
                .containsEntry("call_template_type", "http")
                .containsEntry("http_method", "POST")
                .containsEntry("url", "http://localhost:8080/api/v1/customers")
                .containsEntry("body_field", "body");
    }
}
