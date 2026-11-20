package com.marcosdev7.familia.dto;

import jakarta.validation.constraints.NotNull;

public record CompartilharDespesasDTO(
        @NotNull(message = "Informe se deseja compartilhar as despesas")
        Boolean compartilhar
) {}
