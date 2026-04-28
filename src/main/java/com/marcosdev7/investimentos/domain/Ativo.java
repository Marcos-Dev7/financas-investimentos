package com.marcosdev7.investimentos.domain;

import com.marcosdev7.investimentos.dto.TransacaoRequestDTO;
import com.marcosdev7.usuarios.domain.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
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

    public void atualizarSaldo(Integer quantidade, BigDecimal precoUnitario, BigDecimal taxa, TipoOperacao tipoOperacao) {
        if (tipoOperacao == TipoOperacao.COMPRA) {
            //esse e o objeto atual.
            BigDecimal montanteAtual = BigDecimal.valueOf(quantidadeTotal).multiply(precoMedio);
            //esse e o valor que chega da nova compra.
            BigDecimal novaCompra = BigDecimal.valueOf(quantidade).multiply(precoUnitario).add(taxa);
            //aqui atualiza a quantidade total pra ser usada no precoMedio final.
            this.quantidadeTotal += quantidade;
            this.precoMedio = montanteAtual.add(novaCompra)
                    .divide(BigDecimal.valueOf(quantidadeTotal), 2, RoundingMode.HALF_UP);
        } else {
            //se for venda faz isso.
            if (this.quantidadeTotal < quantidade) {
                throw new RuntimeException("Saldo insuficiente para realizar a venda de " + ticker);
            }
            this.quantidadeTotal -= quantidade;
        }
    }
}
