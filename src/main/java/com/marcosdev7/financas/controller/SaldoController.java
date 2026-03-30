package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.service.SaldoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
