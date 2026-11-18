package com.marcosdev7.financas.domain;

import com.marcosdev7.financas.dto.DespesaRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DespesaTest {

    @Test
    @DisplayName("Deve criar uma despesa com isPaga igual a false por padrão")
    void deveCriarDespesaNaoPagaPorPadrao() {
        LocalDate vencimento = LocalDate.of(2026, 5, 10);
        Despesa despesa = new Despesa(
                "Conta de Luz",
                Mes.MAIO,
                new BigDecimal("180.50"),
                true,
                vencimento,
                "Moradia"
        );

        assertThat(despesa.getDescricao()).isEqualTo("Conta de Luz");
        assertThat(despesa.getMes()).isEqualTo(Mes.MAIO);
        assertThat(despesa.getValor()).isEqualByComparingTo("180.50");
        assertThat(despesa.getIsFixa()).isTrue();
        assertThat(despesa.getDataVencimento()).isEqualTo(vencimento);
        assertThat(despesa.getCategoria()).isEqualTo("Moradia");
        assertThat(despesa.getIsPaga()).isFalse();
    }

    @Test
    @DisplayName("Deve alterar status para pago e permitir estorno")
    void devePagarEEstornarDespesa() {
        Despesa despesa = new Despesa(
                "Internet",
                Mes.MAIO,
                new BigDecimal("120.00"),
                true,
                LocalDate.of(2026, 5, 15),
                "Moradia"
        );

        despesa.pagar();
        assertThat(despesa.getIsPaga()).isTrue();

        despesa.estornar();
        assertThat(despesa.getIsPaga()).isFalse();
    }

    @Test
    @DisplayName("Deve atualizar apenas os campos não nulos em atualizarInformacoes")
    void deveAtualizarApenasCamposNaoNulos() {
        LocalDate vencimentoOriginal = LocalDate.of(2026, 5, 10);
        Despesa despesa = new Despesa(
                "Aluguel",
                Mes.MAIO,
                new BigDecimal("1500.00"),
                true,
                vencimentoOriginal,
                "Moradia"
        );

        DespesaRequestDTO atualizacaoParcial = new DespesaRequestDTO(
                null,
                new BigDecimal("1650.00"),
                null,
                null,
                null,
                null
        );

        despesa.atualizarInformacoes(atualizacaoParcial);

        assertThat(despesa.getDescricao()).isEqualTo("Aluguel");
        assertThat(despesa.getValor()).isEqualByComparingTo("1650.00");
        assertThat(despesa.getMes()).isEqualTo(Mes.MAIO);
        assertThat(despesa.getDataVencimento()).isEqualTo(vencimentoOriginal);
        assertThat(despesa.getIsFixa()).isTrue();
        assertThat(despesa.getCategoria()).isEqualTo("Moradia");
    }

    @Test
    @DisplayName("Deve atualizar todos os campos quando o DTO estiver completo")
    void deveAtualizarTodosOsCampos() {
        Despesa despesa = new Despesa(
                "Aluguel",
                Mes.MAIO,
                new BigDecimal("1500.00"),
                true,
                LocalDate.of(2026, 5, 10),
                "Moradia"
        );

        LocalDate novoVencimento = LocalDate.of(2026, 6, 15);
        DespesaRequestDTO atualizacaoCompleta = new DespesaRequestDTO(
                "Condomínio",
                new BigDecimal("450.00"),
                Mes.JUNHO,
                novoVencimento,
                false,
                "Taxas"
        );

        despesa.atualizarInformacoes(atualizacaoCompleta);

        assertThat(despesa.getDescricao()).isEqualTo("Condomínio");
        assertThat(despesa.getValor()).isEqualByComparingTo("450.00");
        assertThat(despesa.getMes()).isEqualTo(Mes.JUNHO);
        assertThat(despesa.getDataVencimento()).isEqualTo(novoVencimento);
        assertThat(despesa.getIsFixa()).isFalse();
        assertThat(despesa.getCategoria()).isEqualTo("Taxas");
    }
}
