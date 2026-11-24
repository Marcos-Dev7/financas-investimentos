package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmarTotpDTO(
        @NotBlank(message = "Informe sua senha atual")
        String senhaAtual,

        @NotBlank(message = "Informe o código de 6 dígitos do aplicativo autenticador")
        @Pattern(regexp = "\\d{6}", message = "O código deve conter exatamente 6 dígitos numéricos")
        String codigo
) {}
