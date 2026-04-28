package com.marcosdev7.investimentos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marcosdev7.investimentos.domain.TipoOperacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequestDTO(
        @NotBlank
        String ticker,
        @NotNull
        TipoOperacao tipoOperacao,
        @NotNull
        Integer quantidade,
        @PositiveOrZero
        BigDecimal precoUnitario,
        @PositiveOrZero
        BigDecimal taxa,
        @NotNull
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataExecucao
) {}
