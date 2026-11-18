package com.marcosdev7.usuarios.dto;

public record AuthResponseDTO(
        String token,
        Long id,
        String nome,
        String email
) {}
