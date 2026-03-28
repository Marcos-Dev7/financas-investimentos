package com.marcosdev7.financas.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
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

    public Saldo(String descricao, BigDecimal valor, Mes mes, Integer ano) {
        this.descricao = descricao;
        this.valor = valor;
        this.mes = mes;
        this.ano = ano;
    }
}
