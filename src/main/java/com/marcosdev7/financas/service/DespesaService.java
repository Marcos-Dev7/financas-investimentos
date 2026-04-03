package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.dto.DespesaResponseDTO;
import com.marcosdev7.financas.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    public List<DespesaResponseDTO> listarDespesas(){
            return despesaRepository.findAll()
                    .stream()
                    .map(d -> new DespesaResponseDTO(d.getDescricao(), d.getValor(), d.getMes(), d.getDataVencimento(), d.getIsFixa(), d.getCategoria(), d.getIsPaga()))
                    .toList();
    }

    public Despesa encontrarDespesa(Long id){
        var despesaEncontrada = despesaRepository.findById(id).orElseThrow();
        return despesaEncontrada;
    }

    public DespesaResponseDTO cadastrarDespesas(DespesaRequestDTO despesaRequestDTO) {
        Despesa despesa = new Despesa(despesaRequestDTO.descricao(),
                despesaRequestDTO.mes(),
                despesaRequestDTO.valor(),
                despesaRequestDTO.isFixa(),
                despesaRequestDTO.dataVencimento(),
                despesaRequestDTO.categoria());
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
        var resumoDespesasMes = despesaRepository.findByMes(mes);
        var totalSaidas = resumoDespesasMes.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPaga()))
                .map(d -> d.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalSaidas;
    }

    private DespesaResponseDTO paraDTO(Despesa despesa) {
        return new DespesaResponseDTO(
                despesa.getDescricao(),
                despesa.getValor(),
                despesa.getMes(),
                despesa.getDataVencimento(),
                despesa.getIsFixa(),
                despesa.getCategoria(),
                despesa.getIsPaga()
        );
    }
}
