package com.marcosdev7.investimentos.repository;

import com.marcosdev7.investimentos.domain.Dividendo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendoRepository extends JpaRepository<Dividendo, Long> {
}
