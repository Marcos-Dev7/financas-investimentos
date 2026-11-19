package com.marcosdev7.financas.repository;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SaldoRepository extends JpaRepository<Saldo, Long> {
    List<Saldo> findByMes(Mes mes);

    List<Saldo> findByUsuarioId(Long usuarioId);

    Optional<Saldo> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Saldo> findByMesAndUsuarioId(Mes mes, Long usuarioId);

    List<Saldo> findByMesAndUsuarioIdIn(Mes mes, Collection<Long> usuarioIds);
}
