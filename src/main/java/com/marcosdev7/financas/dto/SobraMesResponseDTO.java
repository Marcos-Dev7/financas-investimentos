package com.marcosdev7.financas.dto;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Status;

import java.math.BigDecimal;

public record SobraMesResponseDTO(Mes mes, BigDecimal totalEntradas, BigDecimal totalSaidas, BigDecimal sobraLivre, Status status) {
}
