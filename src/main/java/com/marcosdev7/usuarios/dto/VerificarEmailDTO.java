package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificarEmailDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "O código de verificação é obrigatório")
        @Size(min = 6, max = 6, message = "O código deve conter 6 dígitos")
        String codigo
) {}
