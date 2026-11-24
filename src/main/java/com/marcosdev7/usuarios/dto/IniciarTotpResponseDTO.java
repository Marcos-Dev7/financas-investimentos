package com.marcosdev7.usuarios.dto;

public record IniciarTotpResponseDTO(
        String segredo,
        String otpauthUri,
        String emissor
) {}
