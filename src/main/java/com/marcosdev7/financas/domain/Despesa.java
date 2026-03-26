package com.marcosdev7.financas.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonPropertyOrder({"id", "descricao", "mes", "valor"})
@Entity
@Table(name = "despesas")
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String descricao;
    @Enumerated(EnumType.STRING)
    Mes mes;
    LocalDate dataVencimento;
    Boolean isPaga;
    Boolean isFixa;
    BigDecimal valor;
    String categoria;

    public Despesa(String descricao, Mes mes, BigDecimal valor, Boolean isFixa,LocalDate dataVencimento, String categoria) {
        this.descricao = descricao;
        this.mes = mes;
        this.valor = valor;
        this.isFixa = isFixa;
        this.categoria = categoria;
        this.dataVencimento = dataVencimento;
    }

    public void atualizarInformacoes(DespesaRequestDTO despesaRequestDTO) {
        this.descricao = despesaRequestDTO.descricao();
        this.mes = despesaRequestDTO.mes();
        this.valor = despesaRequestDTO.valor();
        this.dataVencimento = despesaRequestDTO.dataVencimento();
        this.isFixa = despesaRequestDTO.isFixa();
        this.categoria = despesaRequestDTO.categoria();
    }
}
