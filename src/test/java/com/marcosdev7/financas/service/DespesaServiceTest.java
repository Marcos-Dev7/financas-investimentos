package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.dto.DespesaResponseDTO;
import com.marcosdev7.financas.repository.DespesaRepository;
import com.marcosdev7.usuarios.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DespesaServiceTest {

    @Mock
    private DespesaRepository despesaRepository;

    @InjectMocks
    private DespesaService despesaService;

    @Test
    @DisplayName("Deve listar despesas paginadas convertidas para DespesaResponseDTO")
    void deveListarDespesasPaginadas() {
        Pageable pageable = PageRequest.of(0, 10);
        Despesa despesa = new Despesa(
                1L,
                "Internet",
                Mes.MAIO,
                LocalDate.of(2026, 5, 10),
                false,
                true,
                new BigDecimal("120.00"),
                "Moradia"
        );
        when(despesaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(despesa)));

        Page<DespesaResponseDTO> resultado = despesaService.listarDespesas(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        DespesaResponseDTO dto = resultado.getContent().getFirst();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.descricao()).isEqualTo("Internet");
        assertThat(dto.valor()).isEqualByComparingTo("120.00");
        assertThat(dto.mes()).isEqualTo(Mes.MAIO);
        assertThat(dto.isFixa()).isTrue();
        assertThat(dto.isPaga()).isFalse();
        verify(despesaRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Deve encontrar despesa por ID quando existir")
    void deveEncontrarDespesaPorId() {
        Despesa despesa = new Despesa(
                1L, "Luz", Mes.MAIO, LocalDate.of(2026, 5, 15),
                false, true, new BigDecimal("200.00"), "Moradia"
        );
        when(despesaRepository.findById(1L)).thenReturn(Optional.of(despesa));

        Despesa encontrada = despesaService.encontrarDespesa(1L);

        assertThat(encontrada).isSameAs(despesa);
    }

    @Test
    @DisplayName("Deve lançar NoSuchElementException ao buscar despesa inexistente")
    void deveLancarExcecaoQuandoDespesaNaoEncontrada() {
        when(despesaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despesaService.encontrarDespesa(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Deve cadastrar nova despesa com isPaga false e retornar DTO")
    void deveCadastrarDespesa() {
        LocalDate vencimento = LocalDate.of(2026, 5, 20);
        DespesaRequestDTO request = new DespesaRequestDTO(
                "Supermercado",
                new BigDecimal("850.00"),
                Mes.MAIO,
                vencimento,
                false,
                "Alimentação"
        );

        when(despesaRepository.save(any(Despesa.class))).thenAnswer(invocation -> {
            Despesa salva = invocation.getArgument(0);
            return new Despesa(
                    10L,
                    salva.getDescricao(),
                    salva.getMes(),
                    salva.getDataVencimento(),
                    salva.getIsPaga(),
                    salva.getIsFixa(),
                    salva.getValor(),
                    salva.getCategoria()
            );
        });

        DespesaResponseDTO response = despesaService.cadastrarDespesas(request);

        ArgumentCaptor<Despesa> captor = ArgumentCaptor.forClass(Despesa.class);
        verify(despesaRepository).save(captor.capture());
        assertThat(captor.getValue().getIsPaga()).isFalse();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.descricao()).isEqualTo("Supermercado");
        assertThat(response.valor()).isEqualByComparingTo("850.00");
        assertThat(response.mes()).isEqualTo(Mes.MAIO);
        assertThat(response.dataVencimento()).isEqualTo(vencimento);
        assertThat(response.isFixa()).isFalse();
        assertThat(response.categoria()).isEqualTo("Alimentação");
        assertThat(response.isPaga()).isFalse();
    }

    @Test
    @DisplayName("Deve atualizar os dados de uma despesa existente")
    void deveAtualizarDespesa() {
        Despesa existente = new Despesa(
                1L, "Aluguel", Mes.MAIO, LocalDate.of(2026, 5, 5),
                false, true, new BigDecimal("1500.00"), "Moradia"
        );
        DespesaRequestDTO atualizacao = new DespesaRequestDTO(
                "Aluguel Reajustado",
                new BigDecimal("1600.00"),
                null,
                null,
                null,
                null
        );

        when(despesaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(despesaRepository.save(existente)).thenReturn(existente);

        DespesaResponseDTO response = despesaService.atualizarDespesas(1L, atualizacao);

        assertThat(response.descricao()).isEqualTo("Aluguel Reajustado");
        assertThat(response.valor()).isEqualByComparingTo("1600.00");
        assertThat(response.mes()).isEqualTo(Mes.MAIO);
        verify(despesaRepository).save(existente);
    }

    @Test
    @DisplayName("Deve deletar uma despesa existente")
    void deveDeletarDespesa() {
        Despesa existente = new Despesa(
                1L, "Academia", Mes.MAIO, LocalDate.of(2026, 5, 10),
                false, true, new BigDecimal("110.00"), "Saúde"
        );
        when(despesaRepository.findById(1L)).thenReturn(Optional.of(existente));

        despesaService.deletarDespesas(1L);

        verify(despesaRepository).delete(existente);
    }

    @Test
    @DisplayName("Deve marcar uma despesa como paga")
    void devePagarDespesa() {
        Despesa despesa = new Despesa(
                1L, "Luz", Mes.MAIO, LocalDate.of(2026, 5, 10),
                false, true, new BigDecimal("190.00"), "Moradia"
        );
        when(despesaRepository.findById(1L)).thenReturn(Optional.of(despesa));
        when(despesaRepository.save(despesa)).thenReturn(despesa);

        DespesaResponseDTO response = despesaService.pagarDespesa(1L);

        assertThat(response.isPaga()).isTrue();
        assertThat(despesa.getIsPaga()).isTrue();
        verify(despesaRepository).save(despesa);
    }

    @Test
    @DisplayName("Deve estornar uma despesa paga voltando isPaga para false")
    void deveEstornarDespesa() {
        Despesa despesa = new Despesa(
                1L, "Luz", Mes.MAIO, LocalDate.of(2026, 5, 10),
                true, true, new BigDecimal("190.00"), "Moradia"
        );
        when(despesaRepository.findById(1L)).thenReturn(Optional.of(despesa));
        when(despesaRepository.save(despesa)).thenReturn(despesa);

        DespesaResponseDTO response = despesaService.estornarDespesa(1L);

        assertThat(response.isPaga()).isFalse();
        assertThat(despesa.getIsPaga()).isFalse();
        verify(despesaRepository).save(despesa);
    }

    @Test
    @DisplayName("Deve somar apenas as despesas pagas do mês em somarSaidaMes")
    void deveSomarApenasDespesasPagasDoMes() {
        Despesa paga1 = new Despesa(
                1L, "Aluguel", Mes.MAIO, LocalDate.of(2026, 5, 5),
                true, true, new BigDecimal("1500.00"), "Moradia"
        );
        Despesa paga2 = new Despesa(
                2L, "Internet", Mes.MAIO, LocalDate.of(2026, 5, 10),
                true, true, new BigDecimal("120.50"), "Moradia"
        );
        Despesa pendente = new Despesa(
                3L, "Cartão", Mes.MAIO, LocalDate.of(2026, 5, 25),
                false, false, new BigDecimal("900.00"), "Outros"
        );

        when(despesaRepository.findByMes(Mes.MAIO)).thenReturn(List.of(paga1, paga2, pendente));

        BigDecimal totalSaidas = despesaService.somarSaidaMes(Mes.MAIO);

        assertThat(totalSaidas).isEqualByComparingTo("1620.50");
    }

    @Test
    @DisplayName("Deve retornar zero em somarSaidaMes quando não houver despesas pagas")
    void deveRetornarZeroQuandoNaoHouverDespesasPagas() {
        when(despesaRepository.findByMes(Mes.MAIO)).thenReturn(List.of());

        BigDecimal totalSaidas = despesaService.somarSaidaMes(Mes.MAIO);

        assertThat(totalSaidas).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar o total bruto do mês quando o repositório retornar valor")
    void deveRetornarTotalBrutoDoMes() {
        when(despesaRepository.somarTotalPorMes(Mes.MAIO)).thenReturn(new BigDecimal("2520.50"));

        BigDecimal totalBruto = despesaService.somarTotalBruto(Mes.MAIO);

        assertThat(totalBruto).isEqualByComparingTo("2520.50");
    }

    @Test
    @DisplayName("Deve retornar BigDecimal.ZERO em somarTotalBruto quando o repositório retornar null")
    void deveRetornarZeroQuandoTotalBrutoForNull() {
        when(despesaRepository.somarTotalPorMes(Mes.MAIO)).thenReturn(null);

        BigDecimal totalBruto = despesaService.somarTotalBruto(Mes.MAIO);

        assertThat(totalBruto).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Não deve permitir que um membro autenticado edite, pague, estorne ou exclua despesa de outro usuário")
    void naoDevePermitirMutacaoDeDespesaDeOutroUsuario() {
        Usuario membroB = new Usuario(2L, "Ana", "ana@email.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(membroB, null, membroB.getAuthorities())
        );

        try {
            when(despesaRepository.findByIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());
            DespesaRequestDTO req = new DespesaRequestDTO("Alterada", new BigDecimal("10.00"), Mes.DEZEMBRO, LocalDate.now(), false, "Outros");

            assertThatThrownBy(() -> despesaService.atualizarDespesas(100L, req)).isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> despesaService.pagarDespesa(100L)).isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> despesaService.estornarDespesa(100L)).isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> despesaService.deletarDespesas(100L)).isInstanceOf(NoSuchElementException.class);

            verify(despesaRepository, never()).save(any());
            verify(despesaRepository, never()).delete(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
