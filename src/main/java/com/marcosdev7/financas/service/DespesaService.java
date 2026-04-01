package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    public List<Despesa> listarDespesas(){
            return despesaRepository.findAll();
    }

    public Despesa encontrarDespesa(Long id){
        var despesaEncontrada = despesaRepository.findById(id).orElseThrow();
        return despesaEncontrada;
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
        var despesa = encontrarDespesa(id);
        despesa.atualizarInformacoes(despesaRequestDTO);
        return despesaRepository.save(despesa);
    }

    public void deletarDespesas(Long id) {
        var despesa = encontrarDespesa(id);
        despesaRepository.delete(despesa);
    }

    public Despesa pagarDespesa(Long id) {
        var despesa = encontrarDespesa(id);
        despesa.pagar();
        return despesaRepository.save(despesa);
    }

    public Despesa estornarDespesa(Long id) {
        var despesa = encontrarDespesa(id);
        despesa.estornar();
        return despesaRepository.save(despesa);
    }

    public BigDecimal somarSaidaMes(Mes mes){
        var resumoDespesasMes = despesaRepository.findByMes(mes);
        var totalSaidas = resumoDespesasMes.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPaga()))
                .map(d -> d.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalSaidas;
    }
}
