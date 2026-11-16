package com.example.blueprint.infrastructure.ai;

import com.example.blueprint.application.port.InputGuardrail;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * GARDE-FOU d'entrée volontairement simple (pédagogique), appliqué AVANT tout appel au LLM :
 * <ul>
 *   <li>message vide ou trop long (protection coût / déni de service) ;</li>
 *   <li>tentatives classiques d'injection de prompt ("ignore les instructions précédentes"...) ;</li>
 *   <li>demandes destructrices massives ("supprime tous les clients").</li>
 * </ul>
 * En production on compléterait par un modèle de modération et des garde-fous de sortie.
 */
@Component
public class SimplePromptGuardrail implements InputGuardrail {

    public static final int MAX_LENGTH = 2000;

    private static final List<Pattern> PROMPT_INJECTION = List.of(
            Pattern.compile("ignore[rz]?\\s+(all\\s+|toutes\\s+)?(the\\s+|les\\s+|tes\\s+|vos\\s+)?(previous\\s+|prior\\s+|above\\s+)?(instructions|consignes|regles)"),
            Pattern.compile("oublie[rz]?\\s+(toutes\\s+)?(tes|les|vos)\\s+(instructions|consignes|regles)"),
            Pattern.compile("(system\\s*prompt|prompt\\s+systeme|jailbreak|mode\\s+developpeur|developer\\s+mode)"),
            Pattern.compile("(revele|affiche|montre|donne)[a-z]*\\s+(moi\\s+)?(ton|tes|le|la)\\s+(prompt|instructions\\s+internes)"));

    private static final Pattern MASS_DESTRUCTION = Pattern.compile(
            "(supprime[rz]?|efface[rz]?|vide[rz]?|delete|drop)\\s+(tous|toutes|toute|la\\s+totalite|all|la\\s+base|la\\s+collection)");

    @Override
    public Verdict check(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Verdict.deny("le message est vide");
        }
        if (userMessage.length() > MAX_LENGTH) {
            return Verdict.deny("le message dépasse " + MAX_LENGTH + " caractères");
        }
        String normalized = normalize(userMessage);
        if (PROMPT_INJECTION.stream().anyMatch(p -> p.matcher(normalized).find())) {
            return Verdict.deny("tentative de détournement des instructions de l'assistant (prompt injection)");
        }
        if (MASS_DESTRUCTION.matcher(normalized).find()) {
            return Verdict.deny("les suppressions massives ne sont pas autorisées via l'assistant");
        }
        return Verdict.allow();
    }

    /** Minuscules + suppression des accents pour des règles robustes ("règles" == "regles"). */
    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
