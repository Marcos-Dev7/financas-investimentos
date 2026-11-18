package com.marcosdev7.usuarios.security;

import com.marcosdev7.usuarios.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "chave-secreta-de-teste-2026");
    }

    @Test
    @DisplayName("Deve gerar um token JWT válido e extrair o e-mail na validação")
    void deveGerarEValidarTokenJWT() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "senhaHash");

        String token = tokenService.gerarToken(usuario);
        String subject = tokenService.validarToken(token);

        assertThat(token).isNotBlank();
        assertThat(subject).isEqualTo("marcos@email.com");
    }

    @Test
    @DisplayName("Deve retornar null ao validar um token inválido ou adulterado")
    void deveRetornarNullParaTokenInvalido() {
        String subject = tokenService.validarToken("token.invalido.adulterado");
        assertThat(subject).isNull();
    }
}
