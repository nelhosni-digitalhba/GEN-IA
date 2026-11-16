package com.example.blueprint.domain;

import com.example.blueprint.domain.product.Product;
import com.example.blueprint.domain.shared.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agrégat Product")
class ProductTest {

    @Test
    void cree_un_produit_valide() {
        Product p = Product.create("sku-123", "Lampe LED", "Lampe de bureau à intensité réglable");
        assertThat(p.sku().value()).isEqualTo("SKU-123");
        assertThat(p.id()).isNull();
    }

    @Test
    void la_description_doit_etre_assez_riche_pour_la_recherche_semantique() {
        assertThatThrownBy(() -> Product.create("SKU-1", "Lampe", "court"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("au moins " + Product.MIN_DESCRIPTION_LENGTH);
    }

    @Test
    void la_description_est_limitee() {
        String tooLong = "x".repeat(Product.MAX_DESCRIPTION_LENGTH + 1);
        assertThatThrownBy(() -> Product.create("SKU-1", "Lampe", tooLong))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void le_label_est_obligatoire() {
        assertThatThrownBy(() -> Product.create("SKU-1", "  ", "Une description correcte"))
                .hasMessageContaining("label");
    }

    @Test
    void la_mise_a_jour_conserve_le_sku_et_l_identite() {
        Product p = Product.create("SKU-1", "Lampe", "Une description correcte").withId("p1");
        Product updated = p.update("Lampe v2", "Une nouvelle description correcte");
        assertThat(updated.id()).isEqualTo("p1");
        assertThat(updated.sku()).isEqualTo(p.sku());
        assertThat(updated.label()).isEqualTo("Lampe v2");
    }

    @Test
    void le_texte_semantique_combine_label_et_description() {
        Product p = Product.create("SKU-1", "Lampe", "Pour lire le soir");
        assertThat(p.semanticText()).contains("Lampe").contains("Pour lire le soir");
    }
}
