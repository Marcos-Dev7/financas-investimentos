package com.marcosdev7.usuarios.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorSenhaTest {

    @Test
    @DisplayName("Deve aprovar senha forte que atende a todos os requisitos e coincide com a confirmação")
    void deveAprovarSenhaForte() {
        assertThatCode(() -> ValidadorSenha.validarForcaEConfirmacao("Senha@2026", "Senha@2026"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar senha com menos de 8 caracteres")
    void deveRejeitarSenhaCurta() {
        assertThatThrownBy(() -> ValidadorSenha.validarForcaEConfirmacao("Ab1@c", "Ab1@c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 e 100 caracteres");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem letra maiúscula")
    void deveRejeitarSenhaSemMaiuscula() {
        assertThatThrownBy(() -> ValidadorSenha.validarForcaEConfirmacao("senha@2026", "senha@2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maiúscula");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem letra minúscula")
    void deveRejeitarSenhaSemMinuscula() {
        assertThatThrownBy(() -> ValidadorSenha.validarForcaEConfirmacao("SENHA@2026", "SENHA@2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minúscula");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem número")
    void deveRejeitarSenhaSemNumero() {
        assertThatThrownBy(() -> ValidadorSenha.validarForcaEConfirmacao("SenhaForte@", "SenhaForte@"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("número");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem caractere especial")
    void deveRejeitarSenhaSemCaractereEspecial() {
        assertThatThrownBy(() -> ValidadorSenha.validarForcaEConfirmacao("Senha2026", "Senha2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("caractere especial");
    }

    @Test
    @DisplayName("Deve rejeitar quando a confirmação de senha não coincidir")
    void deveRejeitarConfirmacaoDiferente() {
        assertThatThrownBy(() -> ValidadorSenha.validarForcaEConfirmacao("Senha@2026", "Outra@2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não coincide");
    }
}
