package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

public record Configurar2FaSenhaDTO(
        @NotBlank(message = "Informe sua senha atual para confirmar esta alteração de segurança")
        String senhaAtual
) {}
