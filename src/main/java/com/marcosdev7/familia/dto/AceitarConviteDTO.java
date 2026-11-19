package com.marcosdev7.familia.dto;

import jakarta.validation.constraints.NotBlank;

public record AceitarConviteDTO(
        @NotBlank(message = "O token do convite é obrigatório")
        String token
) {}
