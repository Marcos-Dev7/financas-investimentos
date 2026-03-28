package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;


    public List<Despesa> listarDespesas(){
            return despesaRepository.findAll();
    }

    public Despesa cadastrarDespesas(DespesaRequestDTO despesaRequestDTO) {
        Despesa despesa = new Despesa(despesaRequestDTO.descricao(),
                despesaRequestDTO.mes(),
                despesaRequestDTO.valor(),
                despesaRequestDTO.isFixa(),
                despesaRequestDTO.dataVencimento(),
                despesaRequestDTO.categoria());
        return despesaRepository.save(despesa);
    }

    public Despesa atualizarDespesas(Long id, DespesaRequestDTO despesaRequestDTO) {
        var despesaEncontrada = despesaRepository.findById(id).orElseThrow();
        despesaEncontrada.atualizarInformacoes(despesaRequestDTO);
        return despesaRepository.save(despesaEncontrada);
    }

    public void deletarDespesas(Long id) {
        var despesaEncontrada = despesaRepository.findById(id).orElseThrow();
        despesaRepository.deleteById(despesaEncontrada.getId());
    }
}
