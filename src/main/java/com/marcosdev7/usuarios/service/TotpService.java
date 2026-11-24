package com.marcosdev7.usuarios.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Implementação RFC 6238 (TOTP - Time-Based One-Time Password) em Java puro (HmacSHA1, 6 dígitos, janela de 30s).
 * Compatível com Google Authenticator, Authy, Microsoft Authenticator, 1Password e Apple Senhas.
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PERIODO_SEGUNDOS = 30;
    private static final int DIGITOS = 6;
    private static final int JANELA_TOLERANCIA = 1; // +/- 30s para tolerar leve desvio de relógio

    public String gerarSegredoBase32() {
        byte[] bytes = new byte[20]; // 160 bits (padrão recomendado RFC 4226/6238)
        SECURE_RANDOM.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public String gerarOtpAuthUri(String email, String segredoBase32) {
        String emissor = "FinancialFamily";
        String label = URLEncoder.encode(emissor + ":" + email, StandardCharsets.UTF_8);
        String issuerEncoded = URLEncoder.encode(emissor, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + segredoBase32
                + "&issuer=" + issuerEncoded
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PERIODO_SEGUNDOS;
    }

    public boolean validarCodigo(String segredoBase32, String codigoInformado) {
        return validarCodigoNoInstante(segredoBase32, codigoInformado, Instant.now().getEpochSecond());
    }

    public boolean validarCodigoNoInstante(String segredoBase32, String codigoInformado, long epochSeconds) {
        if (segredoBase32 == null || segredoBase32.isBlank() || codigoInformado == null) {
            return false;
        }
        String limpo = codigoInformado.trim();
        if (!limpo.matches("\\d{6}")) {
            return false;
        }

        long passoAtual = epochSeconds / PERIODO_SEGUNDOS;
        byte[] codigoBytes = limpo.getBytes(StandardCharsets.UTF_8);

        for (long delta = -JANELA_TOLERANCIA; delta <= JANELA_TOLERANCIA; delta++) {
            String esperado = gerarCodigoParaPasso(segredoBase32, passoAtual + delta);
            if (MessageDigest.isEqual(codigoBytes, esperado.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    public String gerarCodigoParaInstante(String segredoBase32, long epochSeconds) {
        return gerarCodigoParaPasso(segredoBase32, epochSeconds / PERIODO_SEGUNDOS);
    }

    private String gerarCodigoParaPasso(String segredoBase32, long passo) {
        try {
            byte[] chave = decodificarBase32(segredoBase32);
            byte[] dados = ByteBuffer.allocate(8).putLong(passo).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(chave, "HmacSHA1"));
            byte[] hash = mac.doFinal(dados);

            int offset = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binario % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao calcular código TOTP", e);
        }
    }

    private String codificarBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsNoBuffer = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsNoBuffer += 8;
            while (bitsNoBuffer >= 5) {
                int idx = (buffer >> (bitsNoBuffer - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET.charAt(idx));
                bitsNoBuffer -= 5;
            }
        }
        if (bitsNoBuffer > 0) {
            int idx = (buffer << (5 - bitsNoBuffer)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    private byte[] decodificarBase32(String base32) {
        String limpo = base32.trim().toUpperCase().replace("=", "").replace(" ", "");
        byte[] out = new byte[(limpo.length() * 5) / 8];
        int buffer = 0;
        int bitsNoBuffer = 0;
        int pos = 0;
        for (int i = 0; i < limpo.length(); i++) {
            int val = BASE32_ALPHABET.indexOf(limpo.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Segredo Base32 inválido");
            }
            buffer = (buffer << 5) | val;
            bitsNoBuffer += 5;
            if (bitsNoBuffer >= 8) {
                out[pos++] = (byte) ((buffer >> (bitsNoBuffer - 8)) & 0xFF);
                bitsNoBuffer -= 8;
            }
        }
        return out;
    }
}
