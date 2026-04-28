package com.marcosdev7.investimentos.repository;

import com.marcosdev7.investimentos.domain.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AtivoRepository extends JpaRepository<Ativo, Long> {
    Optional<Ativo>findByTickerAndUsuarioId(String ticker, Long usuarioId);
}
