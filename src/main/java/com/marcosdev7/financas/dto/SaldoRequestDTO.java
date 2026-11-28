package com.marcosdev7.financas.dto;

import com.marcosdev7.financas.domain.Mes;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaldoRequestDTO(@NotBlank @Size(min = 3, max = 100) String descricao,
                              @NotNull @Positive @DecimalMax("999999999.99") BigDecimal valor,
                              @NotNull Mes mes,
                              @NotNull @Min(2000) @Max(2100) Integer ano
) {
}
