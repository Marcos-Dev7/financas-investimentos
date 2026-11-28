package com.marcosdev7.usuarios.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String ISSUER = "financas-investimentos-api";
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${api.security.token.secret:}")
    private String secret;

    private volatile String resolvedSecret;

    @PostConstruct
    public void inicializarChave() {
        resolverChave();
    }

    private String resolverChave() {
        String atual = this.resolvedSecret;
        if (atual != null) {
            return atual;
        }
        synchronized (this) {
            if (this.resolvedSecret != null) {
                return this.resolvedSecret;
            }
            if (secret == null || secret.isBlank()) {
                byte[] randomBytes = new byte[32];
                new SecureRandom().nextBytes(randomBytes);
                this.resolvedSecret = Base64.getEncoder().encodeToString(randomBytes);
                log.error("SEGURANÇA: Variável de ambiente JWT_SECRET não configurada! "
                        + "Uma chave efêmera aleatória de 256 bits foi gerada em memória. "
                        + "Configure JWT_SECRET (>= 32 caracteres) em produção para manter sessões entre reinicializações.");
            } else {
                String trimmed = secret.trim();
                if (trimmed.length() < MIN_SECRET_LENGTH || trimmed.toLowerCase().contains("default-secret")) {
                    throw new IllegalStateException(
                            "Configuração insegura: JWT_SECRET deve ter pelo menos " + MIN_SECRET_LENGTH
                                    + " caracteres e não pode conter valor padrão.");
                }
                this.resolvedSecret = trimmed;
            }
            return this.resolvedSecret;
        }
    }

    public String gerarToken(Usuario usuario) {
        Algorithm algorithm = Algorithm.HMAC256(resolverChave());
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getEmail())
                .withClaim("id", usuario.getId())
                .withClaim("nome", usuario.getNome())
                .withExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    public String validarToken(String tokenJWT) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(resolverChave());
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .acceptLeeway(60)
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}
