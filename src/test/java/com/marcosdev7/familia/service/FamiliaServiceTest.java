package com.marcosdev7.familia.service;

import com.marcosdev7.familia.domain.MembroFamilia;
import com.marcosdev7.familia.domain.StatusFamilia;
import com.marcosdev7.familia.dto.*;
import com.marcosdev7.familia.repository.MembroFamiliaRepository;
import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Saldo;
import com.marcosdev7.financas.domain.Status;
import com.marcosdev7.financas.repository.DespesaRepository;
import com.marcosdev7.financas.repository.SaldoRepository;
import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamiliaServiceTest {

    @Mock
    private MembroFamiliaRepository membroFamiliaRepository;

    @Mock
    private DespesaRepository despesaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private FamiliaService familiaService;

    private final Usuario titular = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash");
    private final Usuario conjuge = new Usuario(2L, "Ana Clara", "ana@email.com", "hash");
    private final Usuario intruso = new Usuario(3L, "Intruso", "intruso@email.com", "hash");

    private void autenticarComo(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve gerar convite com token válido por 48h, titular compartilhando renda e membro desmarcado")
    void deveEnviarConviteComValidade48h() {
        autenticarComo(titular);
        when(membroFamiliaRepository.findByMembroIdAndStatus(1L, StatusFamilia.ACEITO)).thenReturn(List.of());
        when(membroFamiliaRepository.findByTitularIdOrderByDataCriacaoDesc(1L)).thenReturn(List.of());
        when(membroFamiliaRepository.save(any(MembroFamilia.class))).thenAnswer(inv -> inv.getArgument(0));

        MembroFamiliaItemDTO item = familiaService.enviarConvite(new ConviteFamiliaRequestDTO("ana@email.com"));

        assertThat(item.email()).isEqualTo("ana@email.com");
        assertThat(item.status()).isEqualTo(StatusFamilia.PENDENTE);
        assertThat(item.tokenConvite()).isNotBlank();
        assertThat(item.compartilhaRenda()).isFalse();
        assertThat(item.dataExpiracao()).isAfter(LocalDateTime.now().plusHours(47));
        verify(emailService).enviarConviteFamilia(eq("ana@email.com"), eq("Marcos Paulo"), anyString());
    }

    @Test
    @DisplayName("Deve bloquear tentativa de aceitar convite expirado")
    void deveRejeitarConviteExpirado() {
        autenticarComo(conjuge);
        MembroFamilia conviteExpirado = new MembroFamilia(
                titular, "ana@email.com", "token_expirado",
                LocalDateTime.now().minusHours(1), true
        );
        when(membroFamiliaRepository.findByTokenConvite("token_expirado")).thenReturn(Optional.of(conviteExpirado));

        assertThatThrownBy(() -> familiaService.aceitarConvite(new AceitarConviteDTO("token_expirado")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    @DisplayName("Deve bloquear usuário com e-mail diferente ao tentar aceitar convite de outra pessoa")
    void deveBloquearOutroUsuarioDeAceitarConviteAlheio() {
        autenticarComo(intruso);
        MembroFamilia convite = new MembroFamilia(
                titular, "ana@email.com", "token_valido",
                LocalDateTime.now().plusHours(48), true
        );
        when(membroFamiliaRepository.findByTokenConvite("token_valido")).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> familiaService.aceitarConvite(new AceitarConviteDTO("token_valido")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ana@email.com");
    }

    @Test
    @DisplayName("A renda do membro só deve entrar na soma familiar se ele ativar o compartilhamento")
    void deveSomarRendaApenasDeQuemAtivouCompartilhamento() {
        autenticarComo(titular);
        MembroFamilia vinculo = new MembroFamilia(
                10L, titular, conjuge, "ana@email.com", "tok",
                LocalDateTime.now().plusHours(24), StatusFamilia.ACEITO,
                true, false, LocalDateTime.now()
        );

        when(membroFamiliaRepository.findByTitularIdOrderByDataCriacaoDesc(1L)).thenReturn(List.of(vinculo));

        // Cenário 1: membroCompartilhaRenda = false -> busca saldos apenas do titular (ID 1)
        Saldo salarioTitular = new Saldo(1L, "Salário Marcos", new BigDecimal("5000.00"), Mes.DEZEMBRO, 2026);
        when(saldoRepository.findByMesAndUsuarioIdIn(Mes.DEZEMBRO, Set.of(1L)))
                .thenReturn(List.of(salarioTitular));

        Despesa despesaTitular = new Despesa(1L, "Aluguel", Mes.DEZEMBRO, LocalDate.now(), true, true, new BigDecimal("2000.00"), "Moradia");
        despesaTitular.setUsuario(titular);
        Despesa despesaConjuge = new Despesa(2L, "Mercado", Mes.DEZEMBRO, LocalDate.now(), false, false, new BigDecimal("800.00"), "Alimentação");
        despesaConjuge.setUsuario(conjuge);

        when(despesaRepository.findByMesAndUsuarioIdIn(Mes.DEZEMBRO, Set.of(1L, 2L)))
                .thenReturn(List.of(despesaTitular, despesaConjuge));

        ResumoFamiliaResponseDTO resumoSemRendaConjuge = familiaService.calcularResumoFamilia(Mes.DEZEMBRO);

        assertThat(resumoSemRendaConjuge.receitaTotalFamiliar()).isEqualByComparingTo("5000.00");
        assertThat(resumoSemRendaConjuge.despesasPagasFamiliares()).isEqualByComparingTo("2000.00");
        assertThat(resumoSemRendaConjuge.despesasBrutasFamiliares()).isEqualByComparingTo("2800.00");
        assertThat(resumoSemRendaConjuge.saldoDisponivelFamiliar()).isEqualByComparingTo("2200.00");
        assertThat(resumoSemRendaConjuge.saldoCaixaAtualFamiliar()).isEqualByComparingTo("3000.00");
        assertThat(resumoSemRendaConjuge.status()).isEqualTo(Status.POSITIVO);

        // Cenário 2: Cônjuge ativa o compartilhamento de renda (membroCompartilhaRenda = true)
        vinculo.alterarCompartilhamentoMembro(true);
        Saldo salarioConjuge = new Saldo(2L, "Salário Ana", new BigDecimal("4000.00"), Mes.DEZEMBRO, 2026);
        when(saldoRepository.findByMesAndUsuarioIdIn(Mes.DEZEMBRO, Set.of(1L, 2L)))
                .thenReturn(List.of(salarioTitular, salarioConjuge));

        ResumoFamiliaResponseDTO resumoComRendaConjuge = familiaService.calcularResumoFamilia(Mes.DEZEMBRO);

        assertThat(resumoComRendaConjuge.receitaTotalFamiliar()).isEqualByComparingTo("9000.00");
        assertThat(resumoComRendaConjuge.saldoDisponivelFamiliar()).isEqualByComparingTo("6200.00");
    }

    @Test
    @DisplayName("Deve bloquear usuário que não pertence à família ao tentar ver despesas ou resumo familiar")
    void deveBloquearUsuarioForaDaFamilia() {
        autenticarComo(intruso);
        when(membroFamiliaRepository.findByTitularIdOrderByDataCriacaoDesc(3L)).thenReturn(List.of());
        when(membroFamiliaRepository.findByMembroIdAndStatus(3L, StatusFamilia.ACEITO)).thenReturn(List.of());

        assertThatThrownBy(() -> familiaService.listarDespesasFamilia(Mes.DEZEMBRO))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> familiaService.calcularResumoFamilia(Mes.DEZEMBRO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Deve permitir que o titular remova o membro e bloquear terceiro de remover")
    void devePermitirTitularRemoverMembroEBloquearTerceiro() {
        MembroFamilia vinculo = new MembroFamilia(
                10L, titular, conjuge, "ana@email.com", "tok",
                LocalDateTime.now().plusHours(24), StatusFamilia.ACEITO,
                true, true, LocalDateTime.now()
        );
        when(membroFamiliaRepository.findById(10L)).thenReturn(Optional.of(vinculo));

        // Intruso tentando remover -> bloqueado com 403 AccessDeniedException
        autenticarComo(intruso);
        assertThatThrownBy(() -> familiaService.removerMembroOuConvite(10L))
                .isInstanceOf(AccessDeniedException.class);
        verify(membroFamiliaRepository, never()).delete(any());

        // Titular removendo -> sucesso imediato
        autenticarComo(titular);
        familiaService.removerMembroOuConvite(10L);
        verify(membroFamiliaRepository).delete(vinculo);
    }
}
