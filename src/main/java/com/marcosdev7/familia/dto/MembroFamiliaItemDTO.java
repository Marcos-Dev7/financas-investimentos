package com.marcosdev7.familia.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marcosdev7.familia.domain.StatusFamilia;

import java.time.LocalDateTime;

public record MembroFamiliaItemDTO(
        Long vinculoId,
        String nome,
        String email,
        StatusFamilia status,
        Boolean compartilhaRenda,
        Boolean compartilhaDespesas,
        String tokenConvite,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataExpiracao,
        boolean isTitular
) {
    public MembroFamiliaItemDTO(
            Long vinculoId,
            String nome,
            String email,
            StatusFamilia status,
            Boolean compartilhaRenda,
            String tokenConvite,
            LocalDateTime dataExpiracao,
            boolean isTitular) {
        this(vinculoId, nome, email, status, compartilhaRenda, true, tokenConvite, dataExpiracao, isTitular);
    }
}
