package com.example.blueprint.domain;

import com.example.blueprint.domain.product.Sku;
import com.example.blueprint.domain.shared.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Object Sku")
class SkuTest {

    @ParameterizedTest
    @ValueSource(strings = {"SKU-123", "DESK-ERGO-01", "AB", "A1-B2-C3-D4"})
    void accepte_les_formats_valides(String valid) {
        assertThat(Sku.of(valid).value()).isEqualTo(valid);
    }

    @Test
    void normalise_en_majuscules() {
        assertThat(Sku.of(" sku-123 ").value()).isEqualTo("SKU-123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "SKU_123", "SKU 123", "-SKU", "SKU-", "A-B-C-D-E", "TROPLONGSKU1-X"})
    void refuse_les_formats_invalides(String invalid) {
        assertThatThrownBy(() -> Sku.of(invalid)).isInstanceOf(DomainValidationException.class);
    }
}
