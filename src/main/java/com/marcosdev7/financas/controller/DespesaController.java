package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/despesas")
public class DespesaController {

    private final DespesaService despesaService;

    @GetMapping
    public ResponseEntity<List<Despesa>> listarDespesar(){
        return ResponseEntity.ok(despesaService.listarDespesas());
    }

    @PostMapping
    public ResponseEntity<Void> cadastrarDespesa(@RequestBody @Valid DespesaRequestDTO despesaDTO) {
        despesaService.cadastrarDespesas(despesaDTO);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Despesa> atualizarDespesa(@PathVariable Long id, @RequestBody DespesaRequestDTO despesaDTO){
        return ResponseEntity.ok(despesaService.atualizarDespesas(id, despesaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> ApagarDespesa(@PathVariable Long id){
        despesaService.deletarDespesas(id);
        return ResponseEntity.noContent().build();
    }
}
