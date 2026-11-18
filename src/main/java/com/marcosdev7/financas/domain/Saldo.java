package com.marcosdev7.financas.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usuario")
@Entity
@Table(name = "saldos")
public class Saldo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String descricao;
    private BigDecimal valor;
    @Enumerated(EnumType.STRING)
    private Mes mes;
    private Integer ano;

    @Setter
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Saldo(Long id, String descricao, BigDecimal valor, Mes mes, Integer ano) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.mes = mes;
        this.ano = ano;
    }

    public Saldo(String descricao, BigDecimal valor, Mes mes, Integer ano) {
        this.descricao = descricao;
        this.valor = valor;
        this.mes = mes;
        this.ano = ano;
    }

    public void atualizarInformacoes(SaldoRequestDTO saldoRequestDTO) {
        if (saldoRequestDTO.descricao() != null) {this.descricao = saldoRequestDTO.descricao();}
        if (saldoRequestDTO.mes() != null) { this.mes = saldoRequestDTO.mes();}
        if (saldoRequestDTO.valor() != null) {this.valor = saldoRequestDTO.valor();}
        if (saldoRequestDTO.ano() != null) {this.ano = saldoRequestDTO.ano();}
    }
}
