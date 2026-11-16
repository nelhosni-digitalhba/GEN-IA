package com.example.blueprint.domain;

import com.example.blueprint.domain.customer.Customer;
import com.example.blueprint.domain.shared.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agrégat Customer")
class CustomerTest {

    private Customer marie() {
        return Customer.create("Curie", "Marie", "06 01 02 03 04", "marie@science.fr", "1 rue de Paris");
    }

    @Test
    void un_nouveau_client_n_a_pas_encore_d_identifiant() {
        assertThat(marie().id()).isNull();
    }

    @Test
    void le_nom_est_normalise_en_majuscules_et_le_telephone_sans_separateurs() {
        Customer c = marie();
        assertThat(c.nom()).isEqualTo("CURIE");
        assertThat(c.tel()).isEqualTo("0601020304");
        assertThat(c.nomComplet()).isEqualTo("Marie CURIE");
    }

    @Test
    void accepte_un_numero_international() {
        Customer c = Customer.create("Curie", "Marie", "+33 6.01.02.03.04", "marie@science.fr", "Paris");
        assertThat(c.tel()).isEqualTo("+33601020304");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "06-AB-02", "+33 6 01 02 03 04 05 06 07"})
    void refuse_un_telephone_invalide(String tel) {
        assertThatThrownBy(() -> Customer.create("Curie", "Marie", tel, "marie@science.fr", "Paris"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Téléphone invalide");
    }

    @Test
    void tous_les_champs_sont_obligatoires() {
        assertThatThrownBy(() -> Customer.create(" ", "Marie", "0601020304", "m@s.fr", "Paris"))
                .hasMessageContaining("nom");
        assertThatThrownBy(() -> Customer.create("Curie", null, "0601020304", "m@s.fr", "Paris"))
                .hasMessageContaining("prenom");
        assertThatThrownBy(() -> Customer.create("Curie", "Marie", "0601020304", "m@s.fr", ""))
                .hasMessageContaining("adresse");
        assertThatThrownBy(() -> Customer.create("Curie", "Marie", "0601020304", "pas-un-email", "Paris"))
                .hasMessageContaining("Email invalide");
    }

    @Test
    void la_mise_a_jour_conserve_l_identite() {
        Customer saved = marie().withId("42");
        Customer updated = saved.update("Curie", "Marie", "0700000000", "marie@sorbonne.fr", "Sorbonne");
        assertThat(updated.id()).isEqualTo("42");
        assertThat(updated.email().value()).isEqualTo("marie@sorbonne.fr");
    }
}
