package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Status;
import com.marcosdev7.financas.dto.SobraMesResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculoServiceTest {

    @Mock
    private SaldoService saldoService;

    @Mock
    private DespesaService despesaService;

    @InjectMocks
    private CalculoService calculoService;

    @Test
    @DisplayName("Deve retornar Status POSITIVO quando a receita superar as despesas pagas")
    void deveRetornarStatusPositivoQuandoSobraPositiva() {
        when(saldoService.somarTotalEntradas(Mes.MAIO)).thenReturn(new BigDecimal("6000.00"));
        when(despesaService.somarSaidaMes(Mes.MAIO)).thenReturn(new BigDecimal("3500.00"));
        when(despesaService.somarTotalBruto(Mes.MAIO)).thenReturn(new BigDecimal("4200.00"));

        SobraMesResponseDTO resultado = calculoService.calculaSobraLivreMes(Mes.MAIO);

        assertThat(resultado.mes()).isEqualTo(Mes.MAIO);
        assertThat(resultado.receitaTotal()).isEqualByComparingTo("6000.00");
        assertThat(resultado.despesasPagas()).isEqualByComparingTo("3500.00");
        assertThat(resultado.saldoDisponivel()).isEqualByComparingTo("2500.00");
        assertThat(resultado.status()).isEqualTo(Status.POSITIVO);
        assertThat(resultado.despesasBrutas()).isEqualByComparingTo("4200.00");
    }

    @Test
    @DisplayName("Deve retornar Status LIMITE quando o saldo disponível for exatamente zero")
    void deveRetornarStatusLimiteQuandoSaldoZero() {
        when(saldoService.somarTotalEntradas(Mes.JUNHO)).thenReturn(new BigDecimal("4000.00"));
        when(despesaService.somarSaidaMes(Mes.JUNHO)).thenReturn(new BigDecimal("4000.00"));
        when(despesaService.somarTotalBruto(Mes.JUNHO)).thenReturn(new BigDecimal("4500.00"));

        SobraMesResponseDTO resultado = calculoService.calculaSobraLivreMes(Mes.JUNHO);

        assertThat(resultado.saldoDisponivel()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.status()).isEqualTo(Status.LIMITE);
    }

    @Test
    @DisplayName("Deve retornar Status DEFICIT quando as despesas pagas superarem a receita")
    void deveRetornarStatusDeficitQuandoSaldoNegativo() {
        when(saldoService.somarTotalEntradas(Mes.JULHO)).thenReturn(new BigDecimal("3000.00"));
        when(despesaService.somarSaidaMes(Mes.JULHO)).thenReturn(new BigDecimal("3800.00"));
        when(despesaService.somarTotalBruto(Mes.JULHO)).thenReturn(new BigDecimal("3800.00"));

        SobraMesResponseDTO resultado = calculoService.calculaSobraLivreMes(Mes.JULHO);

        assertThat(resultado.saldoDisponivel()).isEqualByComparingTo("-800.00");
        assertThat(resultado.status()).isEqualTo(Status.DEFICIT);
    }
}
