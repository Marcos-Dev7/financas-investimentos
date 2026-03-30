package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Saldo;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.repository.SaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SaldoService {

    private final SaldoRepository saldoRepository;


    public Saldo cadastrarSaldo(SaldoRequestDTO saldoRequestDTO){
        Saldo saldo = new Saldo(saldoRequestDTO.descricao(),
                saldoRequestDTO.valor(),
                saldoRequestDTO.mes(),
                saldoRequestDTO.ano());
        return saldoRepository.save(saldo);
    }

    public BigDecimal somarTotalEntradas(Mes mes){
        var resumoSaldoMes = saldoRepository.findByMes(mes);
        var totalEntradas = resumoSaldoMes.stream().map(saldo -> saldo.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalEntradas;
    }
}
