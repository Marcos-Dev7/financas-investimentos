package com.marcosdev7.financas.repository;

import com.marcosdev7.financas.domain.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
}
