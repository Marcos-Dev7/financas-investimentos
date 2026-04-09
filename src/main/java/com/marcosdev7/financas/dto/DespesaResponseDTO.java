package com.marcosdev7.financas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marcosdev7.financas.domain.Mes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaResponseDTO(Long id,
                                 String descricao,
                                 BigDecimal valor,
                                 Mes mes,
                                 @JsonFormat(pattern = "dd/MM/yyyy")
                                 LocalDate dataVencimento,
                                 Boolean isFixa,
                                 String categoria,
                                 Boolean isPaga) {
}
