package com.marcosdev7.investimentos.domain;

import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(value = EnumType.STRING)
    private TipoOperacao tipoOperacao;
    private Integer quantidade = 0;
    @Column(precision = 18, scale = 2)
    private BigDecimal precoUnitario = BigDecimal.ZERO;
    @Column(precision = 18, scale = 2)
    private BigDecimal taxa = BigDecimal.ZERO;
    private LocalDate dataExecucao;
    @CreatedDate
    private LocalDateTime dataCriacao;
    @ManyToOne
    @JoinColumn(name = "ativo_id")
    private Ativo ativo;
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

}
