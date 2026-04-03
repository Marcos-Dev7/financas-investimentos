package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.dto.DespesaResponseDTO;
import com.marcosdev7.financas.service.DespesaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Despesas", description = "Gestão de gastos fixos e variáveis")
@RequestMapping("/despesas")
public class DespesaController {

    private final DespesaService despesaService;

    @Operation(summary = "Lista todas as despesas",
            description = "Retorna uma lista completa de todas as despesas cadastradas no sistema.")
    @GetMapping
    public ResponseEntity<List<DespesaResponseDTO>> listarDespesar(){
        return ResponseEntity.ok(despesaService.listarDespesas());
    }

    @Operation(summary = "Cadastra uma nova despesa",
            description = "Cria um registro de gasto vinculado a um mês específico. Por padrão, nasce como 'isPaga = false'.")
    @PostMapping
    public ResponseEntity<Void> cadastrarDespesa(@RequestBody @Valid DespesaRequestDTO despesaDTO) {
        despesaService.cadastrarDespesas(despesaDTO);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Atualiza uma despesa",
            description = "Altera os dados de uma despesa existente através do ID.")
    @PutMapping("/{id}")
    public ResponseEntity<DespesaResponseDTO> atualizarDespesa(@PathVariable Long id, @RequestBody DespesaRequestDTO despesaDTO){
        return ResponseEntity.ok(despesaService.atualizarDespesas(id, despesaDTO));
    }

    @Operation(summary = "Dar baixa em uma despesa",
            description = "Altera o status da despesa para Paga, permitindo que ela entre no cálculo da Sobra Livre.")
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<DespesaResponseDTO> pagar(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.pagarDespesa(id));
    }

    @Operation(summary = "Estorna um pagamento",
            description = "Retorna o status da despesa para 'não paga' (isPaga = false).")
    @PatchMapping("/{id}/estornar")
    public ResponseEntity<DespesaResponseDTO> estornar(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.estornarDespesa(id));
    }

    @Operation(summary = "Remove uma despesa",
            description = "Exclui permanentemente o registro da despesa do banco de dados.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> ApagarDespesa(@PathVariable Long id){
        despesaService.deletarDespesas(id);
        return ResponseEntity.noContent().build();
    }
}
