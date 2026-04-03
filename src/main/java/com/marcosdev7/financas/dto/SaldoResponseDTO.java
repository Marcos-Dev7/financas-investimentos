package com.marcosdev7.financas.dto;

import com.marcosdev7.financas.domain.Mes;

import java.math.BigDecimal;

public record SaldoResponseDTO (Long id,
                                String descricao,
                                BigDecimal valor,
                                Mes mes,
                                Integer ano){
}
