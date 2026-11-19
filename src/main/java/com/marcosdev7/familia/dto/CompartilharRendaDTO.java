package com.marcosdev7.familia.dto;

import jakarta.validation.constraints.NotNull;

public record CompartilharRendaDTO(
        @NotNull(message = "Informe se deseja compartilhar a renda")
        Boolean compartilhar
) {}
