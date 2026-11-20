package com.marcosdev7.usuarios.dto;

public record EsqueciSenhaResponseDTO(
        String mensagem,
        boolean emailEnviado,
        boolean smsEnviado
) {
    public EsqueciSenhaResponseDTO(String mensagem, boolean emailEnviado) {
        this(mensagem, emailEnviado, false);
    }
}
