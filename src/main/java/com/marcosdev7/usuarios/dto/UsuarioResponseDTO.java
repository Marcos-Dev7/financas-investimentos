package com.marcosdev7.usuarios.dto;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        Boolean emailVerificado,
        String fotoPerfil
) {
    public UsuarioResponseDTO(Long id, String nome, String email, Boolean emailVerificado) {
        this(id, nome, email, emailVerificado, null);
    }
}
