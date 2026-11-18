package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.repository.DespesaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendadorServiceTest {

    @Mock
    private DespesaRepository despesaRepository;

    @InjectMocks
    private AgendadorService agendadorService;

    @Test
    @DisplayName("Deve projetar despesas fixas do mês anterior para o mês atual com isPaga false e data +1 mês")
    void deveProjetarDespesasFixasParaMesSeguinte() {
        Mes mesAtual = Mes.values()[LocalDate.now().getMonthValue() - 1];
        Mes mesAnterior = mesAtual.anterior();
        LocalDate vencimentoAnterior = LocalDate.of(2026, 1, 15);

        Despesa despesaFixa = new Despesa(
                1L,
                "Aluguel",
                mesAnterior,
                vencimentoAnterior,
                true,
                true,
                new BigDecimal("1800.00"),
                "Moradia"
        );

        when(despesaRepository.findByIsFixaTrueAndMes(mesAnterior)).thenReturn(List.of(despesaFixa));

        agendadorService.projetarDespesasMesSeguinte();

        ArgumentCaptor<Despesa> captor = ArgumentCaptor.forClass(Despesa.class);
        verify(despesaRepository).save(captor.capture());

        Despesa projetada = captor.getValue();
        assertThat(projetada.getDescricao()).isEqualTo("Aluguel");
        assertThat(projetada.getMes()).isEqualTo(mesAtual);
        assertThat(projetada.getValor()).isEqualByComparingTo("1800.00");
        assertThat(projetada.getIsFixa()).isTrue();
        assertThat(projetada.getIsPaga()).isFalse();
        assertThat(projetada.getDataVencimento()).isEqualTo(vencimentoAnterior.plusMonths(1));
        assertThat(projetada.getCategoria()).isEqualTo("Moradia");
    }

    @Test
    @DisplayName("Não deve salvar nenhuma despesa quando não houver despesas fixas no mês anterior")
    void naoDeveSalvarQuandoNaoHouverDespesasFixas() {
        Mes mesAtual = Mes.values()[LocalDate.now().getMonthValue() - 1];
        Mes mesAnterior = mesAtual.anterior();
        when(despesaRepository.findByIsFixaTrueAndMes(mesAnterior)).thenReturn(List.of());

        agendadorService.projetarDespesasMesSeguinte();

        verify(despesaRepository, never()).save(any());
    }
}
