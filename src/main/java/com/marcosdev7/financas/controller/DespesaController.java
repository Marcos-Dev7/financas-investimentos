package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.service.DespesaService;
import com.marcosdev7.financas.service.SaldoService;
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
    private final SaldoService saldoService;

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

    @PatchMapping("/{id}/pagar")
    public ResponseEntity<Despesa> pagar(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.pagarDespesa(id));
    }

    @PatchMapping("/{id}/estornar")
    public ResponseEntity<Despesa> estornar(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.estornarDespesa(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> ApagarDespesa(@PathVariable Long id){
        despesaService.deletarDespesas(id);
        return ResponseEntity.noContent().build();
    }
}
