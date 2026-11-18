package com.marcosdev7.usuarios.service;

import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.dto.AuthResponseDTO;
import com.marcosdev7.usuarios.dto.CadastroUsuarioDTO;
import com.marcosdev7.usuarios.dto.LoginRequestDTO;
import com.marcosdev7.usuarios.dto.UsuarioResponseDTO;
import com.marcosdev7.usuarios.repository.UsuarioRepository;
import com.marcosdev7.usuarios.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthResponseDTO registrar(CadastroUsuarioDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado");
        }

        String senhaCriptografada = passwordEncoder.encode(dto.senha());
        Usuario novoUsuario = new Usuario(dto.nome().trim(), emailNormalizado, senhaCriptografada);
        Usuario salvo = usuarioRepository.save(novoUsuario);

        String token = tokenService.gerarToken(salvo);
        return new AuthResponseDTO(token, salvo.getId(), salvo.getNome(), salvo.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }

        String token = tokenService.gerarToken(usuario);
        return new AuthResponseDTO(token, usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    public UsuarioResponseDTO obterPerfilAtual() {
        Usuario usuario = obterUsuarioAutenticado();
        if (usuario == null) {
            throw new BadCredentialsException("Usuário não autenticado");
        }
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    public static Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }
}
