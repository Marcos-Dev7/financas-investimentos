package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record Verificar2FaDTO(
        @NotBlank(message = "Token do desafio 2FA é obrigatório")
        String desafioToken,

        @NotBlank(message = "Informe o código de 6 dígitos")
        @Pattern(regexp = "\\d{6}", message = "O código deve conter exatamente 6 dígitos numéricos")
        String codigo
) {}
