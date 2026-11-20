package com.marcosdev7.usuarios.repository;

import com.marcosdev7.usuarios.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByEmailRecuperacaoIgnoreCase(String emailRecuperacao);
    Optional<Usuario> findByTelefone(String telefone);
    Optional<Usuario> findByTokenRecuperacao(String tokenRecuperacao);
    boolean existsByEmail(String email);
}
