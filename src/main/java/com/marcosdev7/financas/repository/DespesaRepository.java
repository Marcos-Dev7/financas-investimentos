package com.marcosdev7.financas.repository;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByMes(Mes mes);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.mes = :mes")
    BigDecimal somarTotalPorMes(@Param("mes") Mes mes);

    List<Despesa> findByIsFixaTrueAndMes(Mes mes);
}