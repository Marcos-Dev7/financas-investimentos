package com.marcosdev7.financas.repository;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaldoRepository extends JpaRepository<Saldo, Long> {
    List<Saldo> findByMes(Mes mes);
}
