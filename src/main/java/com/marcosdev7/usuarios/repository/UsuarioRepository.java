package com.marcosdev7.usuarios.repository;

import com.marcosdev7.usuarios.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
