package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Saldo;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.dto.SaldoResponseDTO;
import com.marcosdev7.financas.repository.SaldoRepository;
import com.marcosdev7.usuarios.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaldoServiceTest {

    @Mock
    private SaldoRepository saldoRepository;

    @InjectMocks
    private SaldoService saldoService;

    @Test
    @DisplayName("Deve cadastrar e salvar um novo saldo")
    void deveCadastrarSaldo() {
        SaldoRequestDTO request = new SaldoRequestDTO("Salário", new BigDecimal("6000.00"), Mes.MAIO, 2026);
        Saldo salvo = new Saldo(1L, "Salário", new BigDecimal("6000.00"), Mes.MAIO, 2026);
        when(saldoRepository.save(any(Saldo.class))).thenReturn(salvo);

        Saldo resultado = saldoService.cadastrarSaldo(request);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getDescricao()).isEqualTo("Salário");
        assertThat(resultado.getValor()).isEqualByComparingTo("6000.00");
        assertThat(resultado.getMes()).isEqualTo(Mes.MAIO);
        assertThat(resultado.getAno()).isEqualTo(2026);
        verify(saldoRepository).save(any(Saldo.class));
    }

    @Test
    @DisplayName("Deve somar todas as entradas de um determinado mês")
    void deveSomarTotalEntradasDoMes() {
        Saldo salario = new Saldo(1L, "Salário", new BigDecimal("5000.00"), Mes.MAIO, 2026);
        Saldo freela = new Saldo(2L, "Freelance", new BigDecimal("1250.75"), Mes.MAIO, 2026);
        when(saldoRepository.findByMes(Mes.MAIO)).thenReturn(List.of(salario, freela));

        BigDecimal total = saldoService.somarTotalEntradas(Mes.MAIO);

        assertThat(total).isEqualByComparingTo("6250.75");
    }

    @Test
    @DisplayName("Deve retornar zero ao somar entradas de mês sem registros")
    void deveRetornarZeroQuandoMesNaoTiverEntradas() {
        when(saldoRepository.findByMes(Mes.JANEIRO)).thenReturn(List.of());

        BigDecimal total = saldoService.somarTotalEntradas(Mes.JANEIRO);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve listar todos os saldos convertidos para SaldoResponseDTO")
    void deveListarSaldos() {
        Saldo saldo1 = new Saldo(1L, "Salário", new BigDecimal("5000.00"), Mes.MAIO, 2026);
        Saldo saldo2 = new Saldo(2L, "Extra", new BigDecimal("800.00"), Mes.JUNHO, 2026);
        when(saldoRepository.findAll()).thenReturn(List.of(saldo1, saldo2));

        List<SaldoResponseDTO> lista = saldoService.listarSaldos();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).id()).isEqualTo(1L);
        assertThat(lista.get(0).descricao()).isEqualTo("Salário");
        assertThat(lista.get(1).id()).isEqualTo(2L);
        assertThat(lista.get(1).descricao()).isEqualTo("Extra");
    }

    @Test
    @DisplayName("Deve deletar um saldo existente pelo ID")
    void deveDeletarSaldo() {
        Saldo existente = new Saldo(1L, "Salário", new BigDecimal("5000.00"), Mes.MAIO, 2026);
        when(saldoRepository.findById(1L)).thenReturn(Optional.of(existente));

        saldoService.deletarSaldos(1L);

        verify(saldoRepository).delete(existente);
    }

    @Test
    @DisplayName("Deve lançar NoSuchElementException ao tentar deletar saldo inexistente")
    void deveLancarExcecaoAoDeletarSaldoInexistente() {
        when(saldoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saldoService.deletarSaldos(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Deve atualizar um saldo existente e retornar SaldoResponseDTO")
    void deveAtualizarSaldo() {
        Saldo existente = new Saldo(1L, "Salário", new BigDecimal("5000.00"), Mes.MAIO, 2026);
        SaldoRequestDTO dto = new SaldoRequestDTO("Salário Novo", new BigDecimal("5800.00"), Mes.MAIO, 2026);

        when(saldoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(saldoRepository.save(existente)).thenReturn(existente);

        SaldoResponseDTO atualizado = saldoService.atualizarSaldos(1L, dto);

        assertThat(atualizado.id()).isEqualTo(1L);
        assertThat(atualizado.descricao()).isEqualTo("Salário Novo");
        assertThat(atualizado.valor()).isEqualByComparingTo("5800.00");
        verify(saldoRepository).save(existente);
    }

    @Test
    @DisplayName("Deve lançar NoSuchElementException ao tentar atualizar saldo inexistente")
    void deveLancarExcecaoAoAtualizarSaldoInexistente() {
        SaldoRequestDTO dto = new SaldoRequestDTO("Salário", new BigDecimal("5000.00"), Mes.MAIO, 2026);
        when(saldoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saldoService.atualizarSaldos(99L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Não deve permitir que um membro autenticado edite ou exclua receita de outro usuário")
    void naoDevePermitirMutacaoDeSaldoDeOutroUsuario() {
        Usuario membroB = new Usuario(2L, "Ana", "ana@email.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(membroB, null, membroB.getAuthorities())
        );

        try {
            when(saldoRepository.findByIdAndUsuarioId(200L, 2L)).thenReturn(Optional.empty());
            SaldoRequestDTO dto = new SaldoRequestDTO("Alterado", new BigDecimal("1.00"), Mes.DEZEMBRO, 2026);

            assertThatThrownBy(() -> saldoService.atualizarSaldos(200L, dto)).isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> saldoService.deletarSaldos(200L)).isInstanceOf(NoSuchElementException.class);

            verify(saldoRepository, never()).save(any());
            verify(saldoRepository, never()).delete(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
