package com.marcosdev7.financas.domain;

import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
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

    public void atualizarInformacoes(SaldoRequestDTO saldoRequestDTO) {
        if (saldoRequestDTO.descricao() != null) {this.descricao = saldoRequestDTO.descricao();}
        if (saldoRequestDTO.mes() != null) { this.mes = saldoRequestDTO.mes();}
        if (saldoRequestDTO.valor() != null) {this.valor = saldoRequestDTO.valor();}
        if (saldoRequestDTO.ano() != null) {this.ano = saldoRequestDTO.ano();}
    }
}
