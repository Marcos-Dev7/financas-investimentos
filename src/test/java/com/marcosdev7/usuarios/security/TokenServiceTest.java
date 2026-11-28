package com.marcosdev7.usuarios.security;

import com.marcosdev7.usuarios.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "chave-secreta-de-teste-super-segura-2026-32bytes");
        tokenService.inicializarChave();
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

    @Test
    @DisplayName("Deve rejeitar chave JWT curta ou contendo segredo padrão na inicialização")
    void deveRejeitarChaveFracaOuPadrao() {
        TokenService curto = new TokenService();
        ReflectionTestUtils.setField(curto, "secret", "curta-demais");
        assertThatThrownBy(curto::inicializarChave).isInstanceOf(IllegalStateException.class);

        TokenService padrao = new TokenService();
        ReflectionTestUtils.setField(padrao, "secret", "financial-family-default-secret-key-2026-change-in-prod");
        assertThatThrownBy(padrao::inicializarChave).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve gerar chave efêmera aleatória quando JWT_SECRET estiver vazio (sem chave fixa no código)")
    void deveGerarChaveEfemeraAleatoriaQuandoSecretVazio() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "senhaHash");

        TokenService svc1 = new TokenService();
        ReflectionTestUtils.setField(svc1, "secret", "");
        svc1.inicializarChave();

        TokenService svc2 = new TokenService();
        ReflectionTestUtils.setField(svc2, "secret", "");
        svc2.inicializarChave();

        String token1 = svc1.gerarToken(usuario);
        assertThat(svc1.validarToken(token1)).isEqualTo("marcos@email.com");
        // Chave aleatória distinta por instância garante ausência de segredo estático
        assertThat(svc2.validarToken(token1)).isNull();
    }
}
