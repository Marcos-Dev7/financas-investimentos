package com.marcosdev7.usuarios.service;

import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.dto.AuthResponseDTO;
import com.marcosdev7.usuarios.dto.CadastroUsuarioDTO;
import com.marcosdev7.usuarios.dto.LoginRequestDTO;
import com.marcosdev7.usuarios.repository.UsuarioRepository;
import com.marcosdev7.usuarios.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar um novo usuário com senha criptografada e retornar token JWT")
    void deveRegistrarNovoUsuario() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO("Marcos Paulo", "Marcos@Email.com", "123456");

        when(usuarioRepository.existsByEmail("marcos@email.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash_bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return new Usuario(1L, u.getNome(), u.getEmail(), u.getSenha());
        });
        when(tokenService.gerarToken(any(Usuario.class))).thenReturn("jwt_token_gerado");

        AuthResponseDTO response = authService.registrar(dto);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Marcos Paulo");
        assertThat(response.email()).isEqualTo("marcos@email.com");
        assertThat(response.token()).isEqualTo("jwt_token_gerado");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar registrar e-mail já existente")
    void deveRejeitarEmailDuplicado() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO("Marcos", "marcos@email.com", "123456");
        when(usuarioRepository.existsByEmail("marcos@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este e-mail já está cadastrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve autenticar usuário com credenciais válidas e retornar token JWT")
    void deveRealizarLoginComSucesso() {
        LoginRequestDTO dto = new LoginRequestDTO("marcos@email.com", "123456");
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_bcrypt");

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hash_bcrypt")).thenReturn(true);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt_token_valido");

        AuthResponseDTO response = authService.login(dto);

        assertThat(response.token()).isEqualTo("jwt_token_valido");
        assertThat(response.email()).isEqualTo("marcos@email.com");
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando a senha estiver incorreta")
    void deveRejeitarLoginComSenhaIncorreta() {
        LoginRequestDTO dto = new LoginRequestDTO("marcos@email.com", "senha_errada");
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_bcrypt");

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha_errada", "hash_bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos");
    }
}
