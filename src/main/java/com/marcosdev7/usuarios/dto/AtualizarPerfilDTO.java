package com.marcosdev7.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Email(message = "Formato de e-mail inválido")
        String email,

        String fotoPerfil,

        @Size(max = 20, message = "Telefone inválido")
        String telefone,

        @Email(message = "Formato de e-mail de recuperação inválido")
        String emailRecuperacao
) {
    public AtualizarPerfilDTO(String nome, String email) {
        this(nome, email, null, null, null);
    }

    public AtualizarPerfilDTO(String nome, String email, String fotoPerfil) {
        this(nome, email, fotoPerfil, null, null);
    }
}
