package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.repository.DespesaRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/despesas")
public class DespesaController {

    @Autowired
    private DespesaRepository despesaRepository;

    @GetMapping
    public ResponseEntity<List<Despesa>> listarDespesar(){
        var lista = despesaRepository.findAll();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<Void> cadastrarDespesa(@RequestBody @Valid DespesaRequestDTO despesaDTO) {
        Despesa despesa = new Despesa(despesaDTO.descricao(), despesaDTO.mes(), despesaDTO.valor(), despesaDTO.isFixa(), despesaDTO.dataVencimento(), despesaDTO.categoria());
        despesaRepository.save(despesa);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Despesa> atualizarDespesa(@PathVariable Long id, @RequestBody DespesaRequestDTO despesaDTO){
        var despesaEncontrada = despesaRepository.findById(id).orElseThrow();

        despesaEncontrada.setDescricao(despesaDTO.descricao());
        despesaEncontrada.setValor(despesaDTO.valor());
        despesaEncontrada.setMes(despesaDTO.mes());
        despesaEncontrada.setDataVencimento(despesaDTO.dataVencimento());
        despesaEncontrada.setIsFixa(despesaDTO.isFixa());
        despesaEncontrada.setCategoria(despesaDTO.categoria());

        return ResponseEntity.ok(despesaRepository.save(despesaEncontrada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> ApagarDespesa(@PathVariable Long id){
        var buscarDespesa = despesaRepository.findById(id).orElseThrow();
        despesaRepository.deleteById(buscarDespesa.getId());
        return ResponseEntity.noContent().build();
    }
}
