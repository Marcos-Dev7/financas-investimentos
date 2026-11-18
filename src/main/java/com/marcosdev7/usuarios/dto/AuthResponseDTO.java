package com.marcosdev7.usuarios.dto;

public record AuthResponseDTO(
        String token,
        Long id,
        String nome,
        String email,
        String fotoPerfil
) {
    public AuthResponseDTO(String token, Long id, String nome, String email) {
        this(token, id, nome, email, null);
    }
}
