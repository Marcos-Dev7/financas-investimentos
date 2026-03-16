package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.repository.DespesaRepository;
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
    public ResponseEntity<Void> cadastrarDespesa(@RequestBody Despesa despesa) {
        despesaRepository.save(despesa);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Despesa> atualizarDespesa(@PathVariable Long id, @RequestBody Despesa despesa){
        despesaRepository.findById(id).orElseThrow();
        despesa.setId(id);
        return ResponseEntity.ok(despesaRepository.save(despesa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> ApagarDespesa(@PathVariable Long id){
        var buscarDespesa = despesaRepository.findById(id).orElseThrow();
        despesaRepository.deleteById(buscarDespesa.getId());
        return ResponseEntity.noContent().build();
    }
}
