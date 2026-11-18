package com.marcosdev7.usuarios.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean enviarCodigoVerificacao(String emailDestino, String nomeUsuario, String codigo) {
        if (mailSender == null || mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank()) {
            log.info("============================================================");
            log.info("[MODO DEMO / SEM SMTP CONFIGURADO] Código de verificação para {}: {}", emailDestino, codigo);
            log.info("============================================================");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailUsername);
            helper.setTo(emailDestino);
            helper.setSubject("FinancialFamily — Seu código de verificação: " + codigo);

            String html = """
                    <div style="font-family: Inter, Arial, sans-serif; max-width: 520px; margin: 0 auto; background: #0f172a; color: #f8fafc; padding: 32px; border-radius: 16px; border: 1px solid #334155;">
                        <h2 style="margin-top: 0; color: #38bdf8;">🏦 FinancialFamily</h2>
                        <p style="color: #cbd5e1; font-size: 15px;">Olá, <strong>%s</strong>!</p>
                        <p style="color: #94a3b8; font-size: 14px;">Use o código de 6 dígitos abaixo para confirmar seu e-mail e ativar sua conta:</p>
                        <div style="background: #1e293b; border: 1px solid #3b82f6; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0;">
                            <span style="font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #f8fafc;">%s</span>
                        </div>
                        <p style="color: #64748b; font-size: 12px;">Este código expira em 15 minutos. Se você não solicitou este cadastro, ignore este e-mail.</p>
                    </div>
                    """.formatted(nomeUsuario, codigo);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("E-mail de verificação enviado com sucesso para {}", emailDestino);
            return true;
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail SMTP para {} ({}). Usando fallback de código.", emailDestino, e.getMessage());
            log.info("Código de verificação gerado para {}: {}", emailDestino, codigo);
            return false;
        }
    }
}
