package com.marcosdev7.usuarios.dto;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        Boolean emailVerificado,
        String fotoPerfil,
        String telefone,
        String emailRecuperacao
) {
    public UsuarioResponseDTO(Long id, String nome, String email, Boolean emailVerificado) {
        this(id, nome, email, emailVerificado, null, null, null);
    }

    public UsuarioResponseDTO(Long id, String nome, String email, Boolean emailVerificado, String fotoPerfil) {
        this(id, nome, email, emailVerificado, fotoPerfil, null, null);
    }
}
