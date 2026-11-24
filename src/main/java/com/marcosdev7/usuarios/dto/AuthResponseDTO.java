package com.marcosdev7.usuarios.dto;

public record AuthResponseDTO(
        String token,
        Long id,
        String nome,
        String email,
        String fotoPerfil,
        boolean requerDoisFatores,
        String tipoDoisFatores,
        String desafioToken
) {
    public AuthResponseDTO(String token, Long id, String nome, String email) {
        this(token, id, nome, email, null, false, null, null);
    }

    public AuthResponseDTO(String token, Long id, String nome, String email, String fotoPerfil) {
        this(token, id, nome, email, fotoPerfil, false, null, null);
    }
}
