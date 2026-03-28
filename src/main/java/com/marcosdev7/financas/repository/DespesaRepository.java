package com.marcosdev7.financas.repository;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    Long id(Long id);

    List<Despesa> findByMes(Mes mes);
}
