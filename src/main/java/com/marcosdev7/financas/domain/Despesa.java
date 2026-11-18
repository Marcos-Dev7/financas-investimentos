package com.marcosdev7.financas.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usuario")
@JsonPropertyOrder({"id", "descricao", "mes", "valor"})
@Entity
@Table(name = "despesas")
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String descricao;
    @Enumerated(EnumType.STRING)
    private Mes mes;
    private LocalDate dataVencimento;
    @Column(nullable = false)
    private Boolean isPaga = false;
    private Boolean isFixa;
    private BigDecimal valor;
    private String categoria;

    @Setter
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Despesa(Long id, String descricao, Mes mes, LocalDate dataVencimento, Boolean isPaga, Boolean isFixa, BigDecimal valor, String categoria) {
        this.id = id;
        this.descricao = descricao;
        this.mes = mes;
        this.dataVencimento = dataVencimento;
        this.isPaga = isPaga;
        this.isFixa = isFixa;
        this.valor = valor;
        this.categoria = categoria;
    }

    public Despesa(String descricao, Mes mes, BigDecimal valor, Boolean isFixa, LocalDate dataVencimento, String categoria) {
        this.descricao = descricao;
        this.mes = mes;
        this.valor = valor;
        this.isFixa = isFixa;
        this.categoria = categoria;
        this.dataVencimento = dataVencimento;
    }

    public void atualizarInformacoes(DespesaRequestDTO despesaRequestDTO) {
        if (despesaRequestDTO.descricao() != null) {this.descricao = despesaRequestDTO.descricao();}
        if (despesaRequestDTO.mes() != null) {this.mes = despesaRequestDTO.mes();}
        if (despesaRequestDTO.valor() != null) {this.valor = despesaRequestDTO.valor();}
        if (despesaRequestDTO.dataVencimento() != null) {this.dataVencimento = despesaRequestDTO.dataVencimento();}
        if (despesaRequestDTO.isFixa() != null) {this.isFixa = despesaRequestDTO.isFixa();}
        if (despesaRequestDTO.categoria() != null) {this.categoria = despesaRequestDTO.categoria();}
    }

    public void pagar(){
        this.isPaga = true;
    }

    public void estornar() {
        this.isPaga = false;
    }
}
