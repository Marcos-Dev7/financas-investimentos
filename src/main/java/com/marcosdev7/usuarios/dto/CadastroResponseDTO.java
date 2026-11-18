package com.marcosdev7.usuarios.dto;

public record CadastroResponseDTO(
        String mensagem,
        String email,
        boolean requerVerificacao,
        boolean emailEnviadoViaSmtp,
        String codigoDemonstracao
) {}
