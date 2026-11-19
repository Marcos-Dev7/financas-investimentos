package com.marcosdev7.familia.repository;

import com.marcosdev7.familia.domain.MembroFamilia;
import com.marcosdev7.familia.domain.StatusFamilia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembroFamiliaRepository extends JpaRepository<MembroFamilia, Long> {

    List<MembroFamilia> findByTitularIdOrderByDataCriacaoDesc(Long titularId);

    List<MembroFamilia> findByMembroIdAndStatus(Long membroId, StatusFamilia status);

    List<MembroFamilia> findByEmailConvidadoIgnoreCaseAndStatus(String emailConvidado, StatusFamilia status);

    Optional<MembroFamilia> findByTokenConvite(String tokenConvite);

    boolean existsByTitularIdAndEmailConvidadoIgnoreCase(Long titularId, String emailConvidado);
}
