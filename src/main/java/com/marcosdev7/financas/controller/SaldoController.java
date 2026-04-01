package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Saldo;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.service.SaldoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/saldos")
public class SaldoController {
    private final SaldoService saldoService;

    @PostMapping
    public ResponseEntity<Void> cadastrarSaldo(@RequestBody @Valid SaldoRequestDTO saldoRequestDTO){
        saldoService.cadastrarSaldo(saldoRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<Saldo>> listarSaldos() {
        return ResponseEntity.ok(saldoService.listarSaldos());
    }
}
