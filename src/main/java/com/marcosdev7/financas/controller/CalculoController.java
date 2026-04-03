package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.SobraMesResponseDTO;
import com.marcosdev7.financas.service.CalculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Cálculos", description = "Inteligência financeira e processamento de sobra livre")
@RequestMapping("/calculos")
public class CalculoController {
    private final CalculoService calculosService;

    @Operation(summary = "Calcula a sobra livre do mês",
            description = "Realiza a soma de todas as entradas e subtrai apenas as despesas que já foram marcadas como PAGAS.")
    @GetMapping("/sobra/{mes}")
    public ResponseEntity<SobraMesResponseDTO> listarSobraLivre(@PathVariable Mes mes){
        return ResponseEntity.ok(calculosService.calculaSobraLivreMes(mes));
    }
}
