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
import com.marcosdev7.usuarios.service.AuthService;
import com.marcosdev7.usuarios.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FamiliaService {

    private static final int HORAS_VALIDADE_CONVITE = 48;

    private final MembroFamiliaRepository membroFamiliaRepository;
    private final DespesaRepository despesaRepository;
    private final SaldoRepository saldoRepository;
    private final EmailService emailService;

    @Transactional
    public MembroFamiliaItemDTO enviarConvite(ConviteFamiliaRequestDTO dto) {
        Usuario titular = exigirUsuarioAutenticado();
        String emailConvidado = dto.emailConvidado().trim().toLowerCase();

        if (emailConvidado.equalsIgnoreCase(titular.getEmail())) {
            throw new IllegalArgumentException("Você não pode enviar um convite para o seu próprio e-mail");
        }

        List<MembroFamilia> vinculosComoMembro = membroFamiliaRepository
                .findByMembroIdAndStatus(titular.getId(), StatusFamilia.ACEITO);
        if (!vinculosComoMembro.isEmpty()) {
            throw new IllegalArgumentException("Você já participa de uma família como membro convidado");
        }

        List<MembroFamilia> existentesDoTitular = membroFamiliaRepository
                .findByTitularIdOrderByDataCriacaoDesc(titular.getId());

        Optional<MembroFamilia> conviteExistente = existentesDoTitular.stream()
                .filter(m -> m.getEmailConvidado().equalsIgnoreCase(emailConvidado))
                .findFirst();

        String novoToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime novaExpiracao = LocalDateTime.now().plusHours(HORAS_VALIDADE_CONVITE);

        if (conviteExistente.isPresent()) {
            MembroFamilia vinculo = conviteExistente.get();
            if (vinculo.getStatus() == StatusFamilia.ACEITO) {
                throw new IllegalArgumentException("Este usuário já faz parte da sua família");
            }
            membroFamiliaRepository.delete(vinculo);
            membroFamiliaRepository.flush();
        }

        boolean titularCompartilha = existentesDoTitular.isEmpty()
                || Boolean.TRUE.equals(existentesDoTitular.getFirst().getTitularCompartilhaRenda());

        MembroFamilia novoConvite = new MembroFamilia(
                titular,
                emailConvidado,
                novoToken,
                novaExpiracao,
                titularCompartilha
        );

        MembroFamilia salvo = membroFamiliaRepository.save(novoConvite);
        emailService.enviarConviteFamilia(emailConvidado, titular.getNome(), novoToken);

        return paraItemDTO(salvo, true);
    }

    @Transactional
    public PainelFamiliaResponseDTO aceitarConvite(AceitarConviteDTO dto) {
        Usuario usuarioLogado = exigirUsuarioAutenticado();
        String tokenLimpo = dto.token().trim();

        MembroFamilia convite = membroFamiliaRepository.findByTokenConvite(tokenLimpo)
                .orElseThrow(() -> new IllegalArgumentException("Convite inválido ou não encontrado"));

        if (convite.getStatus() == StatusFamilia.ACEITO) {
            throw new IllegalArgumentException("Este convite já foi aceito anteriormente");
        }

        if (LocalDateTime.now().isAfter(convite.getDataExpiracao())) {
            throw new IllegalArgumentException("Este convite expirou (prazo de 48h). Peça um novo convite ao titular.");
        }

        if (!convite.getEmailConvidado().equalsIgnoreCase(usuarioLogado.getEmail())) {
            throw new AccessDeniedException("Este convite foi enviado exclusivamente para o e-mail " + convite.getEmailConvidado());
        }

        if (convite.getTitular().getId().equals(usuarioLogado.getId())) {
            throw new IllegalArgumentException("O próprio titular não pode aceitar o convite");
        }

        convite.aceitar(usuarioLogado);
        membroFamiliaRepository.save(convite);

        return obterPainelFamilia();
    }

    @Transactional
    public PainelFamiliaResponseDTO alterarCompartilhamentoRenda(CompartilharRendaDTO dto) {
        Usuario usuario = exigirUsuarioAutenticado();
        boolean compartilhar = Boolean.TRUE.equals(dto.compartilhar());

        List<MembroFamilia> comoTitular = membroFamiliaRepository.findByTitularIdOrderByDataCriacaoDesc(usuario.getId());
        if (!comoTitular.isEmpty()) {
            comoTitular.forEach(v -> v.alterarCompartilhamentoTitular(compartilhar));
            membroFamiliaRepository.saveAll(comoTitular);
            return obterPainelFamilia();
        }

        List<MembroFamilia> comoMembro = membroFamiliaRepository.findByMembroIdAndStatus(usuario.getId(), StatusFamilia.ACEITO);
        if (!comoMembro.isEmpty()) {
            comoMembro.forEach(v -> v.alterarCompartilhamentoMembro(compartilhar));
            membroFamiliaRepository.saveAll(comoMembro);
            return obterPainelFamilia();
        }

        throw new AccessDeniedException("Você ainda não participa de uma família ativa");
    }

    @Transactional
    public void removerMembroOuConvite(Long vinculoId) {
        Usuario usuario = exigirUsuarioAutenticado();
        MembroFamilia vinculo = membroFamiliaRepository.findById(vinculoId)
                .orElseThrow(() -> new NoSuchElementException("Vínculo familiar não encontrado"));

        boolean souTitular = vinculo.getTitular().getId().equals(usuario.getId());
        boolean souOProprioMembro = vinculo.getMembro() != null && vinculo.getMembro().getId().equals(usuario.getId());

        if (!souTitular && !souOProprioMembro) {
            throw new AccessDeniedException("Apenas o titular da família pode remover membros");
        }

        membroFamiliaRepository.delete(vinculo);
    }

    public PainelFamiliaResponseDTO obterPainelFamilia() {
        Usuario usuario = exigirUsuarioAutenticado();

        List<MembroFamiliaItemDTO> convitesPendentesParaMim = membroFamiliaRepository
                .findByEmailConvidadoIgnoreCaseAndStatus(usuario.getEmail(), StatusFamilia.PENDENTE)
                .stream()
                .filter(c -> LocalDateTime.now().isBefore(c.getDataExpiracao()))
                .map(c -> new MembroFamiliaItemDTO(
                        c.getId(),
                        c.getTitular().getNome(),
                        c.getTitular().getEmail(),
                        c.getStatus(),
                        c.getTitularCompartilhaRenda(),
                        c.getTokenConvite(),
                        c.getDataExpiracao(),
                        true
                ))
                .toList();

        // Caso 1: Usuário é membro aceito na família de outro titular
        List<MembroFamilia> vinculosComoMembro = membroFamiliaRepository
                .findByMembroIdAndStatus(usuario.getId(), StatusFamilia.ACEITO);

        if (!vinculosComoMembro.isEmpty()) {
            MembroFamilia meuVinculo = vinculosComoMembro.getFirst();
            Usuario titular = meuVinculo.getTitular();
            List<MembroFamilia> todosDaFamilia = membroFamiliaRepository
                    .findByTitularIdOrderByDataCriacaoDesc(titular.getId())
                    .stream()
                    .filter(v -> v.getStatus() == StatusFamilia.ACEITO)
                    .toList();

            List<MembroFamiliaItemDTO> itens = new ArrayList<>();
            itens.add(new MembroFamiliaItemDTO(
                    meuVinculo.getId(),
                    titular.getNome() + " (Titular)",
                    titular.getEmail(),
                    StatusFamilia.ACEITO,
                    meuVinculo.getTitularCompartilhaRenda(),
                    null,
                    null,
                    true
            ));

            todosDaFamilia.forEach(v -> itens.add(paraItemDTO(v, false)));

            return new PainelFamiliaResponseDTO(
                    true,
                    false,
                    titular.getNome(),
                    titular.getEmail(),
                    meuVinculo.getMembroCompartilhaRenda(),
                    itens,
                    convitesPendentesParaMim
            );
        }

        // Caso 2: Usuário é titular (ou ainda não criou vínculos)
        List<MembroFamilia> vinculosDoTitular = membroFamiliaRepository
                .findByTitularIdOrderByDataCriacaoDesc(usuario.getId());

        boolean possuiMembroAceito = vinculosDoTitular.stream()
                .anyMatch(v -> v.getStatus() == StatusFamilia.ACEITO);

        boolean minhaRendaCompartilhada = vinculosDoTitular.isEmpty()
                || Boolean.TRUE.equals(vinculosDoTitular.getFirst().getTitularCompartilhaRenda());

        List<MembroFamiliaItemDTO> listaMembros = vinculosDoTitular.stream()
                .map(v -> paraItemDTO(v, true))
                .toList();

        return new PainelFamiliaResponseDTO(
                possuiMembroAceito,
                true,
                usuario.getNome(),
                usuario.getEmail(),
                minhaRendaCompartilhada,
                listaMembros,
                convitesPendentesParaMim
        );
    }

    public List<DespesaFamiliaResponseDTO> listarDespesasFamilia(Mes mes) {
        Usuario usuario = exigirUsuarioAutenticado();
        ContextoFamiliaAtiva contexto = exigirContextoFamiliaAtiva(usuario);

        List<Despesa> despesas = (mes != null)
                ? despesaRepository.findByMesAndUsuarioIdIn(mes, contexto.todosUsuariosIds())
                : despesaRepository.findByUsuarioIdInOrderByDataVencimentoAsc(contexto.todosUsuariosIds());

        return despesas.stream()
                .sorted(Comparator.comparing(
                        Despesa::getDataVencimento,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(d -> new DespesaFamiliaResponseDTO(
                        d.getId(),
                        d.getDescricao(),
                        d.getValor(),
                        d.getMes(),
                        d.getDataVencimento(),
                        d.getIsFixa(),
                        d.getCategoria(),
                        d.getIsPaga(),
                        d.getUsuario() != null ? d.getUsuario().getNome() : "Família",
                        d.getUsuario() != null && d.getUsuario().getId().equals(usuario.getId())
                ))
                .toList();
    }

    public ResumoFamiliaResponseDTO calcularResumoFamilia(Mes mes) {
        Usuario usuario = exigirUsuarioAutenticado();
        ContextoFamiliaAtiva contexto = exigirContextoFamiliaAtiva(usuario);

        BigDecimal receitaTotal = BigDecimal.ZERO;
        if (!contexto.usuariosComRendaCompartilhadaIds().isEmpty()) {
            receitaTotal = saldoRepository.findByMesAndUsuarioIdIn(mes, contexto.usuariosComRendaCompartilhadaIds())
                    .stream()
                    .map(Saldo::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<Despesa> despesasMes = despesaRepository.findByMesAndUsuarioIdIn(mes, contexto.todosUsuariosIds());

        BigDecimal despesasBrutas = despesasMes.stream()
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasPagas = despesasMes.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPaga()))
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoDisponivel = receitaTotal.subtract(despesasPagas);

        Status status;
        if (saldoDisponivel.compareTo(BigDecimal.ZERO) < 0) {
            status = Status.DEFICIT;
        } else if (saldoDisponivel.compareTo(BigDecimal.ZERO) == 0) {
            status = Status.LIMITE;
        } else {
            status = Status.POSITIVO;
        }

        return new ResumoFamiliaResponseDTO(
                mes,
                receitaTotal,
                despesasPagas,
                saldoDisponivel,
                status,
                despesasBrutas,
                contexto.todosUsuariosIds().size()
        );
    }

    private ContextoFamiliaAtiva exigirContextoFamiliaAtiva(Usuario usuario) {
        // Verifica se o usuário é o titular de uma família com pelo menos 1 membro ACEITO
        List<MembroFamilia> doTitular = membroFamiliaRepository.findByTitularIdOrderByDataCriacaoDesc(usuario.getId())
                .stream()
                .filter(v -> v.getStatus() == StatusFamilia.ACEITO && v.getMembro() != null)
                .toList();

        if (!doTitular.isEmpty()) {
            return montarContexto(usuario, doTitular);
        }

        // Verifica se o usuário é um membro ACEITO na família de outro titular
        List<MembroFamilia> comoMembro = membroFamiliaRepository.findByMembroIdAndStatus(usuario.getId(), StatusFamilia.ACEITO);
        if (!comoMembro.isEmpty()) {
            Usuario titular = comoMembro.getFirst().getTitular();
            List<MembroFamilia> todosAceitos = membroFamiliaRepository.findByTitularIdOrderByDataCriacaoDesc(titular.getId())
                    .stream()
                    .filter(v -> v.getStatus() == StatusFamilia.ACEITO && v.getMembro() != null)
                    .toList();
            return montarContexto(titular, todosAceitos);
        }

        throw new AccessDeniedException("Acesso negado: você não faz parte de uma família ativa");
    }

    private ContextoFamiliaAtiva montarContexto(Usuario titular, List<MembroFamilia> vinculosAceitos) {
        Set<Long> todosIds = new LinkedHashSet<>();
        Set<Long> rendaIds = new LinkedHashSet<>();

        todosIds.add(titular.getId());
        if (Boolean.TRUE.equals(vinculosAceitos.getFirst().getTitularCompartilhaRenda())) {
            rendaIds.add(titular.getId());
        }

        for (MembroFamilia v : vinculosAceitos) {
            if (v.getMembro() != null) {
                todosIds.add(v.getMembro().getId());
                if (Boolean.TRUE.equals(v.getMembroCompartilhaRenda())) {
                    rendaIds.add(v.getMembro().getId());
                }
            }
        }

        return new ContextoFamiliaAtiva(todosIds, rendaIds);
    }

    private MembroFamiliaItemDTO paraItemDTO(MembroFamilia v, boolean exibirToken) {
        String nomeExibicao = (v.getMembro() != null)
                ? v.getMembro().getNome()
                : "Convidado(a) Pendente";

        return new MembroFamiliaItemDTO(
                v.getId(),
                nomeExibicao,
                v.getEmailConvidado(),
                v.getStatus(),
                v.getMembroCompartilhaRenda(),
                (exibirToken && v.getStatus() == StatusFamilia.PENDENTE) ? v.getTokenConvite() : null,
                v.getDataExpiracao(),
                false
        );
    }

    private Usuario exigirUsuarioAutenticado() {
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        if (usuario == null) {
            throw new BadCredentialsException("Usuário não autenticado");
        }
        return usuario;
    }

    private record ContextoFamiliaAtiva(Set<Long> todosUsuariosIds, Set<Long> usuariosComRendaCompartilhadaIds) {}
}
