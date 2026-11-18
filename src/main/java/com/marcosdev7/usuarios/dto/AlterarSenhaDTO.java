package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaDTO(
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 100, message = "A nova senha deve ter no mínimo 8 caracteres")
        String novaSenha,

        @NotBlank(message = "A confirmação da nova senha é obrigatória")
        String confirmacaoNovaSenha
) {}
