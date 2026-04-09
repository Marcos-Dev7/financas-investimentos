package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Status;
import com.marcosdev7.financas.dto.SobraMesResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculoService {

    private final SaldoService saldoService;
    private final DespesaService despesaService;

    public SobraMesResponseDTO calculaSobraLivreMes(Mes mes){
        var entradas = saldoService.somarTotalEntradas(mes);
        var saidas = despesaService.somarSaidaMes(mes);
        var sobraLivre = entradas.subtract(saidas);
        Status status;

        if (sobraLivre.compareTo(BigDecimal.ZERO) < 0) {
            status = Status.DEFICIT;
        } else if (sobraLivre.compareTo(BigDecimal.ZERO) == 0) {
            status = Status.LIMITE;
        } else {
            status = Status.POSITIVO;
        }

        return new SobraMesResponseDTO(mes, entradas, saidas, sobraLivre, status);
    }
}
