package com.marcosdev7.financas.config;

import com.marcosdev7.financas.domain.Mes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterStringParaEnumTest {

    private final ConverterStringParaEnum converter = new ConverterStringParaEnum();

    @Test
    @DisplayName("Deve converter string em minúsculo ou misto para o enum Mes")
    void deveConverterStringParaMes() {
        assertThat(converter.convert("janeiro")).isEqualTo(Mes.JANEIRO);
        assertThat(converter.convert("Outubro")).isEqualTo(Mes.OUTUBRO);
        assertThat(converter.convert("DEZEMBRO")).isEqualTo(Mes.DEZEMBRO);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a string não corresponder a um mês válido")
    void deveLancarExcecaoQuandoMesInvalido() {
        assertThatThrownBy(() -> converter.convert("invalido"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
