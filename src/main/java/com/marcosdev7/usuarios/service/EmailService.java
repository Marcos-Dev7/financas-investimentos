package com.marcosdev7.usuarios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${MAIL_FROM:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean enviarCodigoVerificacao(String emailDestino, String nomeUsuario, String codigo) {
        String assunto = "FinancialFamily — Código de verificação: " + codigo;
        String html = montarTemplateHtml(nomeUsuario, codigo);

        // 1. Prioridade para API HTTPS (Brevo) — funciona no plano gratuito do Render (porta 443)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            String remetente = (mailFrom != null && !mailFrom.isBlank())
                    ? mailFrom.trim()
                    : (mailUsername != null && !mailUsername.isBlank() ? mailUsername.trim() : "noreply@financialfamily.app");

            if (enviarViaBrevoApi(remetente, emailDestino, nomeUsuario, assunto, html)) {
                return true;
            }
        }

        // 2. Fallback para SMTP tradicional (Gmail / Outlook / etc. local)
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String remetente = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : mailUsername.trim();
                helper.setFrom(remetente);
                helper.setTo(emailDestino);
                helper.setSubject(assunto);
                helper.setText(html, true);

                mailSender.send(message);
                log.info("E-mail de verificação enviado via SMTP para {}", emailDestino);
                return true;
            } catch (Exception e) {
                log.warn("Falha no envio SMTP para {}: {}", emailDestino, e.getMessage());
            }
        }

        log.info("============================================================");
        log.info("[AVISO: Configure BREVO_API_KEY + MAIL_FROM para envio real de e-mail]");
        log.info("Código de verificação gerado para {}: {}", emailDestino, codigo);
        log.info("============================================================");
        return false;
    }

    public boolean enviarConviteFamilia(String emailDestino, String nomeTitular, String tokenConvite) {
        String assunto = "FinancialFamily — " + nomeTitular + " convidou você para a Família";
        String html = """
                <div style="font-family: Inter, -apple-system, BlinkMacSystemFont, Arial, sans-serif; max-width: 520px; margin: 0 auto; background: #070b11; color: #f8fafc; padding: 36px 32px; border-radius: 16px; border: 1px solid #1e293b;">
                    <div style="margin-bottom: 24px;">
                        <span style="display: inline-block; background: linear-gradient(135deg, #10b981, #f59e0b); color: #070b11; font-weight: 800; font-size: 13px; padding: 6px 12px; border-radius: 8px; letter-spacing: 0.5px;">FINANCIAL FAMILY</span>
                    </div>
                    <h2 style="margin: 0 0 12px 0; font-size: 22px; color: #f8fafc;">Convite para Família</h2>
                    <p style="color: #cbd5e1; font-size: 15px; line-height: 1.6;"><strong>%s</strong> convidou você para participar da gestão financeira familiar no FinancialFamily.</p>
                    <p style="color: #94a3b8; font-size: 14px; line-height: 1.6;">Acesse sua conta na plataforma, vá na aba <strong>Família</strong> e cole o código de convite abaixo (válido por 48 horas):</p>
                    <div style="background: #111927; border: 1px solid #10b981; border-radius: 12px; padding: 18px; text-align: center; margin: 24px 0;">
                        <span style="font-size: 18px; font-weight: 800; letter-spacing: 2px; color: #34d399; word-break: break-all;">%s</span>
                    </div>
                </div>
                """.formatted(nomeTitular, tokenConvite);

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            String remetente = (mailFrom != null && !mailFrom.isBlank())
                    ? mailFrom.trim()
                    : (mailUsername != null && !mailUsername.isBlank() ? mailUsername.trim() : "noreply@financialfamily.app");
            if (enviarViaBrevoApi(remetente, emailDestino, emailDestino, assunto, html)) {
                return true;
            }
        }

        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                String remetente = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : mailUsername.trim();
                helper.setFrom(remetente);
                helper.setTo(emailDestino);
                helper.setSubject(assunto);
                helper.setText(html, true);
                mailSender.send(message);
                return true;
            } catch (Exception e) {
                log.warn("Falha no envio SMTP de convite familiar para {}: {}", emailDestino, e.getMessage());
            }
        }

        log.info("Convite familiar gerado para {} com token: {}", emailDestino, tokenConvite);
        return false;
    }

    public boolean enviarLinkRecuperacaoSenha(String emailDestino, String nomeUsuario, String linkRecuperacao, String token) {
        String assunto = "FinancialFamily — Recuperação de Senha";
        String html = """
                <div style="font-family: Inter, -apple-system, BlinkMacSystemFont, Arial, sans-serif; max-width: 520px; margin: 0 auto; background: #0A0D12; color: #F0F4F8; padding: 36px 32px; border-radius: 12px; border: 1px solid #1F2937;">
                    <div style="margin-bottom: 24px;">
                        <span style="display: inline-block; background: #059669; color: #FFFFFF; font-weight: 700; font-size: 13px; padding: 6px 12px; border-radius: 6px; letter-spacing: 0.5px;">FINANCIAL FAMILY</span>
                    </div>
                    <h2 style="margin: 0 0 12px 0; font-size: 20px; color: #F0F4F8;">Recuperação de senha</h2>
                    <p style="color: #B4C0D0; font-size: 14px; line-height: 1.6;">Olá, <strong>%s</strong>! Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                    <p style="color: #8B9BB0; font-size: 14px; line-height: 1.6;">Clique no botão abaixo para criar uma nova senha (válido por 30 minutos):</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <a href="%s" style="display: inline-block; background: #059669; color: #FFFFFF; text-decoration: none; font-weight: 600; font-size: 14px; padding: 12px 24px; border-radius: 8px;">Redefinir Minha Senha</a>
                    </div>
                    <p style="color: #8B9BB0; font-size: 13px; line-height: 1.6;">Se preferir, copie o código de recuperação abaixo na tela de login:</p>
                    <div style="background: #11151C; border: 1px solid #2D3B4E; border-radius: 8px; padding: 14px; text-align: center; margin: 14px 0;">
                        <span style="font-size: 14px; font-weight: 700; letter-spacing: 1px; color: #34D399; word-break: break-all;">%s</span>
                    </div>
                    <p style="color: #8B9BB0; font-size: 12px; line-height: 1.5; margin-bottom: 0;">Se você não solicitou esta alteração, ignore este e-mail. Sua senha permanecerá a mesma.</p>
                </div>
                """.formatted(nomeUsuario, linkRecuperacao, token);

        if (enviarEmailHtml(emailDestino, nomeUsuario, assunto, html)) {
            return true;
        }

        log.info("Link de recuperação de senha para {}: {} (token: {})", emailDestino, linkRecuperacao, token);
        return false;
    }

    public boolean enviarSmsCodigoRecuperacao(String telefone, String codigo) {
        if (telefone == null || telefone.isBlank()) {
            return false;
        }

        String numeroE164 = formatarTelefoneE164(telefone);
        String mensagem = "FinancialFamily: Seu codigo de recuperacao de senha e " + codigo + ". Valido por 15 minutos.";

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                Map<String, Object> payload = Map.of(
                        "sender", "FinFamily",
                        "recipient", numeroE164,
                        "content", mensagem,
                        "type", "transactional"
                );
                String jsonBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.brevo.com/v3/transactionalSMS/sms"))
                        .timeout(Duration.ofSeconds(10))
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .header("api-key", brevoApiKey.trim())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("SMS de recuperação enviado via Brevo para {}", numeroE164);
                    return true;
                }
                log.warn("Brevo SMS API retornou status {}: {}", response.statusCode(), response.body());
            } catch (Exception e) {
                log.warn("Erro ao chamar Brevo SMS API para {}: {}", numeroE164, e.getMessage());
            }
        }

        log.info("Código SMS de recuperação gerado para {}: {}", numeroE164, codigo);
        return false;
    }

    private String formatarTelefoneE164(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() == 10 || digitos.length() == 11) {
            return "+55" + digitos;
        }
        return "+" + digitos;
    }

    private boolean enviarEmailHtml(String emailDestino, String nomeDestino, String assunto, String html) {
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            String remetente = (mailFrom != null && !mailFrom.isBlank())
                    ? mailFrom.trim()
                    : (mailUsername != null && !mailUsername.isBlank() ? mailUsername.trim() : "noreply@financialfamily.app");
            if (enviarViaBrevoApi(remetente, emailDestino, nomeDestino, assunto, html)) {
                return true;
            }
        }

        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                String remetente = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : mailUsername.trim();
                helper.setFrom(remetente);
                helper.setTo(emailDestino);
                helper.setSubject(assunto);
                helper.setText(html, true);
                mailSender.send(message);
                return true;
            } catch (Exception e) {
                log.warn("Falha no envio SMTP para {}: {}", emailDestino, e.getMessage());
            }
        }
        return false;
    }

    private boolean enviarViaBrevoApi(String remetenteEmail, String emailDestino, String nomeDestino, String assunto, String html) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", "FinancialFamily", "email", remetenteEmail),
                    "to", List.of(Map.of("email", emailDestino, "name", nomeDestino)),
                    "subject", assunto,
                    "htmlContent", html
            );

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(10))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("E-mail de verificação enviado via Brevo HTTPS API para {}", emailDestino);
                return true;
            } else {
                log.warn("Brevo API retornou status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Erro ao chamar Brevo HTTPS API: {}", e.getMessage());
            return false;
        }
    }

    private String montarTemplateHtml(String nomeUsuario, String codigo) {
        return """
                <div style="font-family: Inter, -apple-system, BlinkMacSystemFont, Arial, sans-serif; max-width: 520px; margin: 0 auto; background: #0b1120; color: #f8fafc; padding: 36px 32px; border-radius: 16px; border: 1px solid #1e293b;">
                    <div style="margin-bottom: 24px;">
                        <span style="display: inline-block; background: linear-gradient(135deg, #2563eb, #10b981); color: #ffffff; font-weight: 800; font-size: 14px; padding: 6px 12px; border-radius: 8px; letter-spacing: 0.5px;">FINANCIAL FAMILY</span>
                    </div>
                    <h2 style="margin: 0 0 12px 0; font-size: 22px; color: #f8fafc;">Confirme seu endereço de e-mail</h2>
                    <p style="color: #cbd5e1; font-size: 15px; line-height: 1.6;">Olá, <strong>%s</strong>!</p>
                    <p style="color: #94a3b8; font-size: 14px; line-height: 1.6;">Utilize o código de segurança de 6 dígitos abaixo para validar sua conta no FinancialFamily:</p>
                    <div style="background: #151f32; border: 1px solid #3b82f6; border-radius: 12px; padding: 22px; text-align: center; margin: 26px 0;">
                        <span style="font-size: 34px; font-weight: 800; letter-spacing: 10px; color: #f8fafc;">%s</span>
                    </div>
                    <p style="color: #64748b; font-size: 12px; line-height: 1.5; margin-bottom: 0;">Este código é válido por 15 minutos. Caso você não tenha solicitado este cadastro, basta ignorar esta mensagem.</p>
                </div>
                """.formatted(nomeUsuario, codigo);
    }
}
