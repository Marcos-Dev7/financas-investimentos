package com.marcosdev7.financas.domain;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class Despesa {
    Long id;
    String descricao;
    Mes mes;
    BigDecimal valor;
}
