package com.marcosdev7.financas.dto;

import com.marcosdev7.financas.domain.Mes;

import java.math.BigDecimal;

public record SobraMesResponseDTO(Mes mes, BigDecimal totalEntradas, BigDecimal totalSaidas, BigDecimal sobraLivre) {
}
