package com.marcosdev7.usuarios.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    @DisplayName("Deve gerar segredo Base32 de 32 caracteres e URI otpauth:// válida")
    void deveGerarSegredoEUriValida() {
        String segredo = totpService.gerarSegredoBase32();
        String uri = totpService.gerarOtpAuthUri("marcos@email.com", segredo);

        assertThat(segredo).matches("[A-Z2-7]{32}");
        assertThat(uri)
                .startsWith("otpauth://totp/")
                .contains("secret=" + segredo)
                .contains("issuer=FinancialFamily")
                .contains("digits=6")
                .contains("period=30");
    }

    @Test
    @DisplayName("Deve validar código TOTP gerado no mesmo período e dentro da janela de +/- 30s")
    void deveValidarCodigoDentroDaJanela() {
        String segredo = totpService.gerarSegredoBase32();
        long instanteBase = 1_760_000_000L;

        String codigo = totpService.gerarCodigoParaInstante(segredo, instanteBase);

        assertThat(codigo).matches("\\d{6}");
        assertThat(totpService.validarCodigoNoInstante(segredo, codigo, instanteBase)).isTrue();
        assertThat(totpService.validarCodigoNoInstante(segredo, codigo, instanteBase + 30)).isTrue();
        assertThat(totpService.validarCodigoNoInstante(segredo, codigo, instanteBase - 30)).isTrue();
        // Fora da janela (+/- 90s)
        assertThat(totpService.validarCodigoNoInstante(segredo, codigo, instanteBase + 90)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar códigos malformados ou incorretos")
    void deveRejeitarCodigosInvalidos() {
        String segredo = totpService.gerarSegredoBase32();
        long instanteBase = 1_760_000_000L;

        assertThat(totpService.validarCodigoNoInstante(segredo, "abc123", instanteBase)).isFalse();
        assertThat(totpService.validarCodigoNoInstante(segredo, "12345", instanteBase)).isFalse();
        assertThat(totpService.validarCodigoNoInstante(segredo, "", instanteBase)).isFalse();
        assertThat(totpService.validarCodigoNoInstante(segredo, null, instanteBase)).isFalse();
    }
}
