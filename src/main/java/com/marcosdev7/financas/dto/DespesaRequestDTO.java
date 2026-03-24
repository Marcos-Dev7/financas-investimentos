package com.marcosdev7.financas.dto;

import com.marcosdev7.financas.domain.Mes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaRequestDTO (
        @NotBlank
        @Size(min = 3, max = 100)
        String descricao,
        @Positive
        BigDecimal valor,
        @NotNull
        Mes mes,
        @NotNull
        LocalDate dataVencimento,
        Boolean isFixa,
        String categoria ) {}
