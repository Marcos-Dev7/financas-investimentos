package com.marcosdev7.financas.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MesTest {

    @Test
    @DisplayName("Deve retornar o próximo mês corretamente em sequência normal")
    void deveRetornarProximoMes() {
        assertThat(Mes.JANEIRO.proximo()).isEqualTo(Mes.FEVEREIRO);
        assertThat(Mes.JUNHO.proximo()).isEqualTo(Mes.JULHO);
    }

    @Test
    @DisplayName("Deve virar o ciclo de DEZEMBRO para JANEIRO ao chamar proximo()")
    void deveVirarCicloDeDezembroParaJaneiro() {
        assertThat(Mes.DEZEMBRO.proximo()).isEqualTo(Mes.JANEIRO);
    }

    @Test
    @DisplayName("Deve retornar o mês anterior corretamente em sequência normal")
    void deveRetornarMesAnterior() {
        assertThat(Mes.FEVEREIRO.anterior()).isEqualTo(Mes.JANEIRO);
        assertThat(Mes.DEZEMBRO.anterior()).isEqualTo(Mes.NOVEMBRO);
    }

    @Test
    @DisplayName("Deve virar o ciclo de JANEIRO para DEZEMBRO ao chamar anterior()")
    void deveVirarCicloDeJaneiroParaDezembro() {
        assertThat(Mes.JANEIRO.anterior()).isEqualTo(Mes.DEZEMBRO);
    }

    @Test
    @DisplayName("Deve converter string ignorando maiúsculas/minúsculas via fromString")
    void deveConverterFromStringCaseInsensitive() {
        assertThat(Mes.fromString("janeiro")).isEqualTo(Mes.JANEIRO);
        assertThat(Mes.fromString("MARCO")).isEqualTo(Mes.MARCO);
        assertThat(Mes.fromString("Setembro")).isEqualTo(Mes.SETEMBRO);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para mês inválido no fromString")
    void deveLancarExcecaoParaMesInvalido() {
        assertThatThrownBy(() -> Mes.fromString("mes_invalido"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve retornar o nome do mês em minúsculo no getMesCase")
    void deveRetornarMesCaseEmMinusculo() {
        assertThat(Mes.ABRIL.getMesCase()).isEqualTo("abril");
        assertThat(Mes.NOVEMBRO.getMesCase()).isEqualTo("novembro");
    }
}
