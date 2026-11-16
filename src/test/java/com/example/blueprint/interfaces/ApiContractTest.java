package com.example.blueprint.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-FIRST : garantit que le code implémente EXACTEMENT le contrat api-spec.yaml.
 * Si quelqu'un ajoute une route au contrat sans l'implémenter (ou l'inverse), ce test échoue.
 */
class ApiContractTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch");

    @Test
    void chaque_operation_du_contrat_est_implementee_et_reciproquement() throws Exception {
        assertThat(implementedOperations()).isEqualTo(contractOperations());
    }

    @SuppressWarnings("unchecked")
    private Set<String> contractOperations() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/openapi/api-spec.yaml")) {
            Map<String, Object> spec = new Yaml().load(in);
            Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) spec.get("paths");
            Set<String> ops = new TreeSet<>();
            paths.forEach((path, item) -> item.keySet().stream()
                    .filter(HTTP_METHODS::contains)
                    .forEach(m -> ops.add(m.toUpperCase() + " " + path)));
            return ops;
        }
    }

    private Set<String> implementedOperations() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> ops = new TreeSet<>();
        for (var bd : scanner.findCandidateComponents("com.example.blueprint.interfaces")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
            String prefix = base == null || base.path().length == 0 ? "" : base.path()[0];
            for (Method m : type.getDeclaredMethods()) {
                RequestMapping rm = AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class);
                if (rm == null) {
                    continue;
                }
                String sub = rm.path().length == 0 ? "" : rm.path()[0];
                Set<String> verbs = new HashSet<>();
                for (var verb : rm.method()) {
                    verbs.add(verb.name());
                }
                verbs.forEach(v -> ops.add(v + " " + prefix + sub));
            }
        }
        return ops;
    }
}
