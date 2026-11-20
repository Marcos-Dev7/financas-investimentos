package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequestDTO(
        @NotBlank(message = "Selecione o meio de recuperação")
        String canal,

        @NotBlank(message = "Informe o e-mail ou telefone cadastrado")
        String identificador
) {
}
