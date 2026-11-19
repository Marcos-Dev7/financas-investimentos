package com.marcosdev7.familia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConviteFamiliaRequestDTO(
        @NotBlank(message = "O e-mail do convidado é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String emailConvidado
) {}
