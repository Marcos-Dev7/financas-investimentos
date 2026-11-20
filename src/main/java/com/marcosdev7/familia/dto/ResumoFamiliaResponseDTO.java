package com.marcosdev7.familia.dto;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Status;

import java.math.BigDecimal;

public record ResumoFamiliaResponseDTO(
        Mes mes,
        BigDecimal receitaTotalFamiliar,
        BigDecimal despesasPagasFamiliares,
        BigDecimal saldoDisponivelFamiliar,
        Status status,
        BigDecimal despesasBrutasFamiliares,
        int totalMembrosAtivos,
        BigDecimal saldoCaixaAtualFamiliar
) {
    public ResumoFamiliaResponseDTO(
            Mes mes,
            BigDecimal receitaTotalFamiliar,
            BigDecimal despesasPagasFamiliares,
            BigDecimal saldoDisponivelFamiliar,
            Status status,
            BigDecimal despesasBrutasFamiliares,
            int totalMembrosAtivos) {
        this(mes, receitaTotalFamiliar, despesasPagasFamiliares, saldoDisponivelFamiliar, status, despesasBrutasFamiliares, totalMembrosAtivos, saldoDisponivelFamiliar);
    }
}
