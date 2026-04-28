package com.marcosdev7.investimentos.repository;

import com.marcosdev7.investimentos.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
}
