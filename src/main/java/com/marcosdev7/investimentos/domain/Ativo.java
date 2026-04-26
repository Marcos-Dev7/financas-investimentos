package com.marcosdev7.investimentos.domain;

import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ativo")
public class Ativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticker;
    @Enumerated(value = EnumType.STRING)
    private TipoAtivo tipoAtivo;
    private Integer quantidadeTotal = 0;
    @Column(precision = 18, scale = 2)
    private BigDecimal precoMedio = BigDecimal.ZERO;
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
