package com.marcosdev7.usuarios.dto;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        Boolean emailVerificado,
        String fotoPerfil,
        String telefone,
        String emailRecuperacao,
        Boolean doisFatoresAtivo,
        String tipoDoisFatores
) {
    public UsuarioResponseDTO(Long id, String nome, String email, Boolean emailVerificado) {
        this(id, nome, email, emailVerificado, null, null, null, false, null);
    }

    public UsuarioResponseDTO(Long id, String nome, String email, Boolean emailVerificado, String fotoPerfil) {
        this(id, nome, email, emailVerificado, fotoPerfil, null, null, false, null);
    }

    public UsuarioResponseDTO(Long id, String nome, String email, Boolean emailVerificado, String fotoPerfil, String telefone, String emailRecuperacao) {
        this(id, nome, email, emailVerificado, fotoPerfil, telefone, emailRecuperacao, false, null);
    }
}
