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
    }

    public void confirmarEmail() {
        this.emailVerificado = true;
        this.codigoVerificacao = null;
        this.codigoExpiracao = null;
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
