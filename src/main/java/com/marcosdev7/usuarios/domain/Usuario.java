package com.marcosdev7.usuarios.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    private String senha;

    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;

    @Column(name = "codigo_verificacao", length = 10)
    private String codigoVerificacao;

    @Column(name = "codigo_expiracao")
    private LocalDateTime codigoExpiracao;

    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "email_recuperacao")
    private String emailRecuperacao;

    @Column(name = "token_recuperacao", length = 100)
    private String tokenRecuperacao;

    @Column(name = "token_recuperacao_expiracao")
    private LocalDateTime tokenRecuperacaoExpiracao;

    @Column(name = "dois_fatores_ativo", nullable = false)
    private Boolean doisFatoresAtivo = false;

    @Column(name = "tipo_dois_fatores", length = 20)
    private String tipoDoisFatores;

    @Column(name = "segredo_totp", length = 64)
    private String segredoTotp;

    @Column(name = "segredo_totp_pendente", length = 64)
    private String segredoTotpPendente;

    @Column(name = "desafio_2fa_token", length = 100)
    private String desafio2FaToken;

    @Column(name = "codigo_2fa", length = 10)
    private String codigo2Fa;

    @Column(name = "codigo_2fa_expiracao")
    private LocalDateTime codigo2FaExpiracao;

    @Column(name = "tentativas_2fa", nullable = false)
    private Integer tentativas2Fa = 0;

    @Column(name = "tentativas_login", nullable = false)
    private Integer tentativasLogin = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "tentativas_verificacao_email", nullable = false)
    private Integer tentativasVerificacaoEmail = 0;

    public Usuario(Long id, String nome, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.emailVerificado = true;
    }

    public Usuario(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.emailVerificado = false;
    }

    public void definirCodigoVerificacao(String codigo, LocalDateTime expiracao) {
        this.codigoVerificacao = codigo;
        this.codigoExpiracao = expiracao;
        this.emailVerificado = false;
        this.tentativasVerificacaoEmail = 0;
    }

    public void confirmarEmail() {
        this.emailVerificado = true;
        this.codigoVerificacao = null;
        this.codigoExpiracao = null;
        this.tentativasVerificacaoEmail = 0;
    }

    public void incrementarTentativaVerificacaoEmail() {
        this.tentativasVerificacaoEmail = (this.tentativasVerificacaoEmail == null ? 0 : this.tentativasVerificacaoEmail) + 1;
    }

    public void invalidarCodigoVerificacao() {
        this.codigoVerificacao = null;
        this.codigoExpiracao = null;
        this.tentativasVerificacaoEmail = 0;
    }

    public boolean estaBloqueadoParaLogin(LocalDateTime agora) {
        return this.bloqueadoAte != null && agora.isBefore(this.bloqueadoAte);
    }

    public void registrarFalhaLogin(int maxTentativas, int minutosBloqueio) {
        int atual = (this.tentativasLogin == null ? 0 : this.tentativasLogin) + 1;
        if (atual >= maxTentativas) {
            this.tentativasLogin = 0;
            this.bloqueadoAte = LocalDateTime.now().plusMinutes(minutosBloqueio);
        } else {
            this.tentativasLogin = atual;
        }
    }

    public void limparFalhasLogin() {
        this.tentativasLogin = 0;
        this.bloqueadoAte = null;
    }

    public void atualizarNome(String novoNome) {
        if (novoNome != null && !novoNome.isBlank()) {
            this.nome = novoNome.trim();
        }
    }

    public void atualizarEmail(String novoEmail) {
        if (novoEmail != null && !novoEmail.isBlank()) {
            this.email = novoEmail.trim().toLowerCase();
        }
    }

    public void atualizarFotoPerfil(String novaFoto) {
        this.fotoPerfil = novaFoto;
    }

    public void atualizarSenha(String novaSenhaCriptografada) {
        this.senha = novaSenhaCriptografada;
    }

    public void atualizarTelefone(String novoTelefone) {
        if (novoTelefone == null || novoTelefone.isBlank()) {
            this.telefone = null;
        } else {
            this.telefone = novoTelefone.replaceAll("\\D", "");
        }
    }

    public void atualizarEmailRecuperacao(String novoEmailRecuperacao) {
        if (novoEmailRecuperacao == null || novoEmailRecuperacao.isBlank()) {
            this.emailRecuperacao = null;
        } else {
            this.emailRecuperacao = novoEmailRecuperacao.trim().toLowerCase();
        }
    }

    public void definirTokenRecuperacao(String token, LocalDateTime expiracao) {
        this.tokenRecuperacao = token;
        this.tokenRecuperacaoExpiracao = expiracao;
    }

    public void limparTokenRecuperacao() {
        this.tokenRecuperacao = null;
        this.tokenRecuperacaoExpiracao = null;
    }

    public void ativar2FaEmail() {
        this.doisFatoresAtivo = true;
        this.tipoDoisFatores = "EMAIL";
        this.segredoTotp = null;
        this.segredoTotpPendente = null;
        limparDesafio2Fa();
    }

    public void prepararTotpPendente(String segredo) {
        this.segredoTotpPendente = segredo;
    }

    public void confirmarTotp() {
        this.segredoTotp = this.segredoTotpPendente;
        this.segredoTotpPendente = null;
        this.doisFatoresAtivo = true;
        this.tipoDoisFatores = "APP";
        limparDesafio2Fa();
    }

    public void desativar2Fa() {
        this.doisFatoresAtivo = false;
        this.tipoDoisFatores = null;
        this.segredoTotp = null;
        this.segredoTotpPendente = null;
        limparDesafio2Fa();
    }

    public void iniciarDesafio2Fa(String tokenDesafio, String codigoEmail, LocalDateTime expiracao) {
        this.desafio2FaToken = tokenDesafio;
        this.codigo2Fa = codigoEmail;
        this.codigo2FaExpiracao = expiracao;
        this.tentativas2Fa = 0;
    }

    public void incrementarTentativa2Fa() {
        this.tentativas2Fa = (this.tentativas2Fa == null ? 0 : this.tentativas2Fa) + 1;
    }

    public void limparDesafio2Fa() {
        this.desafio2FaToken = null;
        this.codigo2Fa = null;
        this.codigo2FaExpiracao = null;
        this.tentativas2Fa = 0;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
