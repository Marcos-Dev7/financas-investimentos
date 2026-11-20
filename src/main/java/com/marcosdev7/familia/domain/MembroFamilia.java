package com.marcosdev7.familia.domain;

import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "membro_familia")
public class MembroFamilia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "titular_id", nullable = false)
    private Usuario titular;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "membro_id")
    private Usuario membro;

    @Column(name = "email_convidado", nullable = false, length = 100)
    private String emailConvidado;

    @Column(name = "token_convite", nullable = false, unique = true, length = 64)
    private String tokenConvite;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFamilia status = StatusFamilia.PENDENTE;

    @Column(name = "titular_compartilha_renda", nullable = false)
    private Boolean titularCompartilhaRenda = true;

    @Column(name = "membro_compartilha_renda", nullable = false)
    private Boolean membroCompartilhaRenda = false;

    @Column(name = "titular_compartilha_despesas", nullable = false)
    private Boolean titularCompartilhaDespesas = true;

    @Column(name = "membro_compartilha_despesas", nullable = false)
    private Boolean membroCompartilhaDespesas = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    public MembroFamilia(Long id, Usuario titular, Usuario membro, String emailConvidado, String tokenConvite,
                         LocalDateTime dataExpiracao, StatusFamilia status, Boolean titularCompartilhaRenda,
                         Boolean membroCompartilhaRenda, LocalDateTime dataCriacao) {
        this(id, titular, membro, emailConvidado, tokenConvite, dataExpiracao, status,
                titularCompartilhaRenda, membroCompartilhaRenda, true, true, dataCriacao);
    }

    public MembroFamilia(Long id, Usuario titular, Usuario membro, String emailConvidado, String tokenConvite,
                         LocalDateTime dataExpiracao, StatusFamilia status, Boolean titularCompartilhaRenda,
                         Boolean membroCompartilhaRenda, Boolean titularCompartilhaDespesas,
                         Boolean membroCompartilhaDespesas, LocalDateTime dataCriacao) {
        this.id = id;
        this.titular = titular;
        this.membro = membro;
        this.emailConvidado = emailConvidado;
        this.tokenConvite = tokenConvite;
        this.dataExpiracao = dataExpiracao;
        this.status = status;
        this.titularCompartilhaRenda = titularCompartilhaRenda;
        this.membroCompartilhaRenda = membroCompartilhaRenda;
        this.titularCompartilhaDespesas = titularCompartilhaDespesas;
        this.membroCompartilhaDespesas = membroCompartilhaDespesas;
        this.dataCriacao = dataCriacao;
    }

    public MembroFamilia(Usuario titular, String emailConvidado, String tokenConvite, LocalDateTime dataExpiracao, boolean titularCompartilhaRenda) {
        this(titular, emailConvidado, tokenConvite, dataExpiracao, titularCompartilhaRenda, true);
    }

    public MembroFamilia(Usuario titular, String emailConvidado, String tokenConvite, LocalDateTime dataExpiracao,
                         boolean titularCompartilhaRenda, boolean titularCompartilhaDespesas) {
        this.titular = titular;
        this.emailConvidado = emailConvidado.trim().toLowerCase();
        this.tokenConvite = tokenConvite;
        this.dataExpiracao = dataExpiracao;
        this.status = StatusFamilia.PENDENTE;
        this.titularCompartilhaRenda = titularCompartilhaRenda;
        this.membroCompartilhaRenda = false;
        this.titularCompartilhaDespesas = titularCompartilhaDespesas;
        this.membroCompartilhaDespesas = true;
        this.dataCriacao = LocalDateTime.now();
    }

    public void aceitar(Usuario usuarioConvidado) {
        this.membro = usuarioConvidado;
        this.status = StatusFamilia.ACEITO;
    }

    public void alterarCompartilhamentoTitular(boolean compartilhar) {
        this.titularCompartilhaRenda = compartilhar;
    }

    public void alterarCompartilhamentoMembro(boolean compartilhar) {
        this.membroCompartilhaRenda = compartilhar;
    }

    public void alterarCompartilhamentoDespesasTitular(boolean compartilhar) {
        this.titularCompartilhaDespesas = compartilhar;
    }

    public void alterarCompartilhamentoDespesasMembro(boolean compartilhar) {
        this.membroCompartilhaDespesas = compartilhar;
    }
}
