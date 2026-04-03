package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.dto.SaldoResponseDTO;
import com.marcosdev7.financas.service.SaldoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Saldos", description = "Gestão de entradas e receitas familiares")
@RequestMapping("/saldos")
public class SaldoController {
    private final SaldoService saldoService;

    @Operation(summary = "Cadastra uma nova renda", description = "Registra um novo valor de entrada (Salário, Extra, etc) vinculado a um mês.")
    @PostMapping
    public ResponseEntity<Void> cadastrarSaldo(@RequestBody @Valid SaldoRequestDTO saldoRequestDTO){
        saldoService.cadastrarSaldo(saldoRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Lista todos os saldos",
            description = "Retorna todas as fontes de renda cadastradas.")
    @GetMapping
    public ResponseEntity<List<SaldoResponseDTO>> listarSaldos() {
        return ResponseEntity.ok(saldoService.listarSaldos());
    }
}
