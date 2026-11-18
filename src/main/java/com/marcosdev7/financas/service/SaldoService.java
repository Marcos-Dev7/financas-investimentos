package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Saldo;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.dto.SaldoResponseDTO;
import com.marcosdev7.financas.repository.SaldoRepository;
import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaldoService {

    private final SaldoRepository saldoRepository;

    public Saldo cadastrarSaldo(SaldoRequestDTO saldoRequestDTO){
        Saldo saldo = new Saldo(saldoRequestDTO.descricao(),
                saldoRequestDTO.valor(),
                saldoRequestDTO.mes(),
                saldoRequestDTO.ano());

        Usuario usuario = AuthService.obterUsuarioAutenticado();
        if (usuario != null) {
            saldo.setUsuario(usuario);
        }

        return saldoRepository.save(saldo);
    }

    public BigDecimal somarTotalEntradas(Mes mes){
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        var resumoSaldoMes = (usuario != null)
                ? saldoRepository.findByMesAndUsuarioId(mes, usuario.getId())
                : saldoRepository.findByMes(mes);

        return resumoSaldoMes.stream()
                .map(Saldo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<SaldoResponseDTO> listarSaldos() {
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        var saldos = (usuario != null)
                ? saldoRepository.findByUsuarioId(usuario.getId())
                : saldoRepository.findAll();

        return saldos.stream()
                .map(this::paraDTO)
                .toList();
    }

    public void deletarSaldos(Long id) {
        var saldo = encontrarSaldo(id);
        saldoRepository.delete(saldo);
    }

    public SaldoResponseDTO atualizarSaldos(Long id, SaldoRequestDTO saldoRequestDTO) {
        var saldo = encontrarSaldo(id);
        saldo.atualizarInformacoes(saldoRequestDTO);
        return paraDTO(saldoRepository.save(saldo));
    }

    private Saldo encontrarSaldo(Long id) {
        Usuario usuario = AuthService.obterUsuarioAutenticado();
        if (usuario != null) {
            return saldoRepository.findByIdAndUsuarioId(id, usuario.getId()).orElseThrow();
        }
        return saldoRepository.findById(id).orElseThrow();
    }

    private SaldoResponseDTO paraDTO(Saldo saldo){
        return new SaldoResponseDTO(saldo.getId(), saldo.getDescricao(), saldo.getValor(), saldo.getMes(), saldo.getAno());
    }
}
