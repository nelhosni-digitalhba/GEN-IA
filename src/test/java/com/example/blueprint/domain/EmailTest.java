package com.example.blueprint.domain;

import com.example.blueprint.domain.customer.Email;
import com.example.blueprint.domain.shared.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Object Email")
class EmailTest {

    @Test
    void normalise_en_minuscules_et_supprime_les_espaces() {
        assertThat(Email.of("  Marie@Science.FR ").value()).isEqualTo("marie@science.fr");
    }

    @Test
    void deux_emails_de_meme_valeur_sont_egaux() {
        assertThat(Email.of("a@b.fr")).isEqualTo(Email.of("A@B.FR"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sans-arobase.fr", "a@b", "@domaine.fr", "a b@c.fr"})
    void refuse_les_emails_invalides(String invalid) {
        assertThatThrownBy(() -> Email.of(invalid)).isInstanceOf(DomainValidationException.class);
    }
}
