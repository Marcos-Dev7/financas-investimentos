package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/despesas")
public class DespesaController {

    @Autowired
    private DespesaRepository despesaRepository;

    @GetMapping
    public List<Despesa> listarDespesar(){
        return despesaRepository.findAll();
    }

    @PostMapping
    public Despesa cadastrarDespesa(@RequestBody Despesa despesa) {
        return despesaRepository.save(despesa);
    }

    @PutMapping("/{id}")
    public Despesa atualizarDespesa(@PathVariable Long id, @RequestBody Despesa despesa){
        despesaRepository.findById(id).orElseThrow();
        despesa.setId(id);
        return despesaRepository.save(despesa);
    }

    @DeleteMapping("/{id}")
    public void ApagarDespesa(@PathVariable Long id){
        var buscarDespesa = despesaRepository.findById(id).orElseThrow();
        despesaRepository.deleteById(buscarDespesa.getId());
    }
}
