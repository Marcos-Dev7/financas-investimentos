package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.dto.DespesaResponseDTO;
import com.marcosdev7.financas.repository.DespesaRepository;
import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> CAMPOS_ORDENACAO_PERMITIDOS = Set.of(
            "dataVencimento", "valor", "descricao", "categoria", "isPaga", "mes", "id"
    );

    private final DespesaRepository despesaRepository;

    public Page<DespesaResponseDTO> listarDespesas(Pageable paginacao){
        Pageable paginacaoSegura = sanitizarPaginacao(paginacao);
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        if (usuario != null) {
            return despesaRepository.findByUsuarioId(usuario.getId(), paginacaoSegura)
                    .map(this::paraDTO);
        }
        return despesaRepository.findAll(paginacaoSegura)
                .map(this::paraDTO);
    }

    private Pageable sanitizarPaginacao(Pageable paginacao) {
        if (paginacao == null || paginacao.isUnpaged()) {
            return PageRequest.of(0, 10, Sort.by("dataVencimento"));
        }
        int page = Math.max(0, paginacao.getPageNumber());
        int size = Math.min(Math.max(1, paginacao.getPageSize()), MAX_PAGE_SIZE);

        List<Sort.Order> ordensValidas = paginacao.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO_PERMITIDOS.contains(order.getProperty()))
                .toList();

        Sort sortSeguro = ordensValidas.isEmpty()
                ? (paginacao.getSort().isUnsorted() ? Sort.unsorted() : Sort.by("dataVencimento"))
                : Sort.by(ordensValidas);

        return PageRequest.of(page, size, sortSeguro);
    }

    public Despesa encontrarDespesa(Long id){
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        if (usuario != null) {
            return despesaRepository.findByIdAndUsuarioId(id, usuario.getId()).orElseThrow();
        }
        return despesaRepository.findById(id).orElseThrow();
    }

    public DespesaResponseDTO cadastrarDespesas(DespesaRequestDTO despesaRequestDTO) {
        Despesa despesa = new Despesa(despesaRequestDTO.descricao(),
                despesaRequestDTO.mes(),
                despesaRequestDTO.valor(),
                despesaRequestDTO.isFixa(),
                despesaRequestDTO.dataVencimento(),
                despesaRequestDTO.categoria());

        Usuario usuario = AuthService.obterUsuarioAutenticado();
        if (usuario != null) {
            despesa.setUsuario(usuario);
        }

        return paraDTO(despesaRepository.save(despesa));
    }

    public DespesaResponseDTO atualizarDespesas(Long id, DespesaRequestDTO despesaRequestDTO) {
        var despesa = encontrarDespesa(id);
        despesa.atualizarInformacoes(despesaRequestDTO);
        return paraDTO(despesaRepository.save(despesa));
    }

    public void deletarDespesas(Long id) {
        var despesa = encontrarDespesa(id);
        despesaRepository.delete(despesa);
    }

    public DespesaResponseDTO pagarDespesa(Long id) {
        var despesa = encontrarDespesa(id);
        despesa.pagar();
        return paraDTO(despesaRepository.save(despesa));
    }

    public DespesaResponseDTO estornarDespesa(Long id) {
        var despesa = encontrarDespesa(id);
        despesa.estornar();
        return paraDTO(despesaRepository.save(despesa));
    }

    public BigDecimal somarSaidaMes(Mes mes){
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        var resumoDespesasMes = (usuario != null)
                ? despesaRepository.findByMesAndUsuarioId(mes, usuario.getId())
                : despesaRepository.findByMes(mes);

        return resumoDespesasMes.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPaga()))
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DespesaResponseDTO paraDTO(Despesa despesa) {
        return new DespesaResponseDTO(
                despesa.getId(),
                despesa.getDescricao(),
                despesa.getValor(),
                despesa.getMes(),
                despesa.getDataVencimento(),
                despesa.getIsFixa(),
                despesa.getCategoria(),
                despesa.getIsPaga()
        );
    }

    public BigDecimal somarTotalBruto(Mes mes) {
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        var resultado = (usuario != null)
                ? despesaRepository.somarTotalPorMesEUsuario(mes, usuario.getId())
                : despesaRepository.somarTotalPorMes(mes);

        if (resultado == null){
            return BigDecimal.ZERO;
        } else {
            return resultado;
        }
    }

}
