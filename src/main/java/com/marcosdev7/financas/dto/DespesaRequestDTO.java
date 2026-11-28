package com.marcosdev7.financas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marcosdev7.financas.domain.Mes;
import jakarta.validation.constraints.DecimalMax;
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
        @NotNull
        @Positive
        @DecimalMax("999999999.99")
        BigDecimal valor,
        @NotNull
        Mes mes,
        @NotNull
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataVencimento,
        Boolean isFixa,
        @Size(max = 50)
        String categoria ) {}
