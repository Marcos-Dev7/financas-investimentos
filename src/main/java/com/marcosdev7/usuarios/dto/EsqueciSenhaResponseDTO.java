package com.marcosdev7.usuarios.dto;

public record EsqueciSenhaResponseDTO(
        String mensagem,
        boolean emailEnviado
) {
}
