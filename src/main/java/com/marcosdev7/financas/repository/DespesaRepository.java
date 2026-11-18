package com.marcosdev7.financas.repository;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByMes(Mes mes);

    Page<Despesa> findByUsuarioId(Long usuarioId, Pageable pageable);

    Optional<Despesa> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Despesa> findByMesAndUsuarioId(Mes mes, Long usuarioId);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.mes = :mes")
    BigDecimal somarTotalPorMes(@Param("mes") Mes mes);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.mes = :mes AND d.usuario.id = :usuarioId")
    BigDecimal somarTotalPorMesEUsuario(@Param("mes") Mes mes, @Param("usuarioId") Long usuarioId);

    List<Despesa> findByIsFixaTrueAndMes(Mes mes);
}
