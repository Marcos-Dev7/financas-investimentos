package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.service.CalculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calculos")
public class CalculoController {
    private final CalculoService calculosService;

    @GetMapping("/sobra/{mes}")
    public ResponseEntity<BigDecimal> listarSobraLivre(@PathVariable Mes mes){
        return ResponseEntity.ok(calculosService.calculaSobraLivreMes(mes));
    }
}
