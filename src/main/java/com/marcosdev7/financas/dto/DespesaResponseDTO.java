package com.marcosdev7.financas.dto;

import com.marcosdev7.financas.domain.Mes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaResponseDTO(
                                 String descricao,
                                 BigDecimal valor,
                                 Mes mes,
                                 LocalDate dataVencimento,
                                 Boolean isFixa,
                                 String categoria,
                                 Boolean isPaga) {
}
