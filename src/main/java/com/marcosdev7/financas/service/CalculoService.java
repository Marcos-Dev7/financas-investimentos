package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Mes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculoService {

    private final SaldoService saldoService;
    private final DespesaService despesaService;

    public BigDecimal calculaSobraLivreMes(Mes mes){
        var entradas = saldoService.somarTotalEntradas(mes);
        var saidas = despesaService.somarSaidaMes(mes);

        return entradas.subtract(saidas);
    }
}
