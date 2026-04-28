package com.marcosdev7.investimentos.service;

import com.marcosdev7.investimentos.domain.Ativo;
import com.marcosdev7.investimentos.domain.Transacao;
import com.marcosdev7.investimentos.dto.TransacaoRequestDTO;
import com.marcosdev7.investimentos.repository.AtivoRepository;
import com.marcosdev7.investimentos.repository.TransacaoRepository;
import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class TransacaoService {
    private final AtivoRepository ativoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransacaoRepository transacaoRepository;

    @Transactional
    public void registrarTransacao(TransacaoRequestDTO transacaoRequestDTO, Long usuarioId) {
        Usuario usuarioExistente = usuarioRepository
                .findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
        Ativo ativoParaAtualizar = ativoRepository.findByTickerAndUsuarioId(transacaoRequestDTO.ticker(), usuarioId)
                .orElseGet(() -> {
                   Ativo novoAtivo = new Ativo();
                   novoAtivo.setTicker(transacaoRequestDTO.ticker());
                   novoAtivo.setUsuario(usuarioExistente);
                   return novoAtivo;
                });
        ativoParaAtualizar.atualizarSaldo(
                transacaoRequestDTO.quantidade(),
                transacaoRequestDTO.precoUnitario(),
                transacaoRequestDTO.taxa(),
                transacaoRequestDTO.tipoOperacao()
        );

        ativoRepository.save(ativoParaAtualizar);

        Transacao novaTransacao = new Transacao();
        novaTransacao.setAtivo(ativoParaAtualizar);
        novaTransacao.setUsuario(usuarioExistente);
        novaTransacao.setTipoOperacao(transacaoRequestDTO.tipoOperacao());
        novaTransacao.setQuantidade(transacaoRequestDTO.quantidade());
        novaTransacao.setPrecoUnitario(transacaoRequestDTO.precoUnitario());
        novaTransacao.setTaxa(transacaoRequestDTO.taxa());
        novaTransacao.setDataExecucao(transacaoRequestDTO.dataExecucao());

        transacaoRepository.save(novaTransacao);

    }
}

