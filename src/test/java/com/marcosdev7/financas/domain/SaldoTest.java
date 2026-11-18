package com.marcosdev7.financas.domain;

import com.marcosdev7.financas.dto.SaldoRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SaldoTest {

    @Test
    @DisplayName("Deve criar um saldo com todos os atributos preenchidos")
    void deveCriarSaldoCorretamente() {
        Saldo saldo = new Saldo("Salário Mensal", new BigDecimal("5000.00"), Mes.ABRIL, 2026);

        assertThat(saldo.getDescricao()).isEqualTo("Salário Mensal");
        assertThat(saldo.getValor()).isEqualByComparingTo("5000.00");
        assertThat(saldo.getMes()).isEqualTo(Mes.ABRIL);
        assertThat(saldo.getAno()).isEqualTo(2026);
    }

    @Test
    @DisplayName("Deve atualizar apenas campos não nulos em atualizarInformacoes")
    void deveAtualizarApenasCamposNaoNulos() {
        Saldo saldo = new Saldo("Salário Mensal", new BigDecimal("5000.00"), Mes.ABRIL, 2026);

        SaldoRequestDTO dtoParcial = new SaldoRequestDTO(null, new BigDecimal("5500.00"), null, null);
        saldo.atualizarInformacoes(dtoParcial);

        assertThat(saldo.getDescricao()).isEqualTo("Salário Mensal");
        assertThat(saldo.getValor()).isEqualByComparingTo("5500.00");
        assertThat(saldo.getMes()).isEqualTo(Mes.ABRIL);
        assertThat(saldo.getAno()).isEqualTo(2026);
    }

    @Test
    @DisplayName("Deve atualizar todos os campos quando o DTO estiver completo")
    void deveAtualizarTodosOsCampos() {
        Saldo saldo = new Saldo("Salário Mensal", new BigDecimal("5000.00"), Mes.ABRIL, 2026);

        SaldoRequestDTO dtoCompleto = new SaldoRequestDTO("Bônus Anual", new BigDecimal("2000.00"), Mes.MAIO, 2027);
        saldo.atualizarInformacoes(dtoCompleto);

        assertThat(saldo.getDescricao()).isEqualTo("Bônus Anual");
        assertThat(saldo.getValor()).isEqualByComparingTo("2000.00");
        assertThat(saldo.getMes()).isEqualTo(Mes.MAIO);
        assertThat(saldo.getAno()).isEqualTo(2027);
    }
}
