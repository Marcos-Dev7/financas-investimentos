package com.marcosdev7.usuarios.service;

import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.dto.*;
import com.marcosdev7.usuarios.repository.UsuarioRepository;
import com.marcosdev7.usuarios.security.TokenService;
import com.marcosdev7.usuarios.validation.ValidadorSenha;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MINUTOS_EXPIRACAO_CODIGO = 15;
    private static final int MAX_FOTO_BASE64_LEN = 750_000;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;

    @Transactional
    public CadastroResponseDTO registrar(CadastroUsuarioDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();

        ValidadorSenha.validarForcaEConfirmacao(dto.senha(), dto.confirmacaoSenha());

        Usuario usuarioExistente = usuarioRepository.findByEmail(emailNormalizado).orElse(null);
        if (usuarioExistente != null) {
            if (Boolean.TRUE.equals(usuarioExistente.getEmailVerificado())) {
                throw new IllegalArgumentException("Este e-mail já está cadastrado");
            }
            usuarioExistente.atualizarNome(dto.nome().trim());
            usuarioExistente.atualizarSenha(passwordEncoder.encode(dto.senha()));
            return gerarEEnviarCodigo(usuarioExistente);
        }

        String senhaCriptografada = passwordEncoder.encode(dto.senha());
        Usuario novoUsuario = new Usuario(dto.nome().trim(), emailNormalizado, senhaCriptografada);

        return gerarEEnviarCodigo(novoUsuario);
    }

    @Transactional
    public AuthResponseDTO verificarEmail(VerificarEmailDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();
        String codigoLimpo = dto.codigo().trim();

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para o e-mail informado"));

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            String token = tokenService.gerarToken(usuario);
            return paraAuthResponse(token, usuario);
        }

        if (usuario.getCodigoVerificacao() == null || !usuario.getCodigoVerificacao().equals(codigoLimpo)) {
            throw new IllegalArgumentException("Código de verificação inválido");
        }

        if (usuario.getCodigoExpiracao() == null || LocalDateTime.now().isAfter(usuario.getCodigoExpiracao())) {
            throw new IllegalArgumentException("O código de verificação expirou. Solicite um novo código.");
        }

        usuario.confirmarEmail();
        usuarioRepository.save(usuario);

        String token = tokenService.gerarToken(usuario);
        return paraAuthResponse(token, usuario);
    }

    @Transactional
    public CadastroResponseDTO reenviarCodigo(ReenviarCodigoDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma conta encontrada com este e-mail"));

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new IllegalArgumentException("Este e-mail já foi verificado. Você já pode fazer login.");
        }

        return gerarEEnviarCodigo(usuario);
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        String emailNormalizado = dto.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }

        if (Boolean.FALSE.equals(usuario.getEmailVerificado())) {
            throw new IllegalArgumentException("EMAIL_NAO_VERIFICADO:" + usuario.getEmail());
        }

        String token = tokenService.gerarToken(usuario);
        return paraAuthResponse(token, usuario);
    }

    public UsuarioResponseDTO obterPerfilAtual() {
        Usuario autenticado = exigirUsuarioAutenticado();
        Usuario usuario = usuarioRepository.findById(autenticado.getId()).orElse(autenticado);
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getEmailVerificado(),
                usuario.getFotoPerfil()
        );
    }

    @Transactional
    public AuthResponseDTO atualizarPerfil(AtualizarPerfilDTO dto) {
        Usuario autenticado = exigirUsuarioAutenticado();
        Usuario usuario = usuarioRepository.findById(autenticado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        usuario.atualizarNome(dto.nome());

        if (dto.email() != null && !dto.email().isBlank()) {
            String novoEmail = dto.email().trim().toLowerCase();
            if (!novoEmail.equals(usuario.getEmail())) {
                if (usuarioRepository.existsByEmail(novoEmail)) {
                    throw new IllegalArgumentException("Este e-mail já está em uso por outra conta");
                }
                usuario.atualizarEmail(novoEmail);
            }
        }

        if (dto.fotoPerfil() != null) {
            if (dto.fotoPerfil().isBlank() || "REMOVER".equals(dto.fotoPerfil())) {
                usuario.atualizarFotoPerfil(null);
            } else {
                if (dto.fotoPerfil().length() > MAX_FOTO_BASE64_LEN) {
                    throw new IllegalArgumentException("A imagem selecionada é muito grande. Escolha uma foto menor.");
                }
                usuario.atualizarFotoPerfil(dto.fotoPerfil());
            }
        }

        Usuario salvo = usuarioRepository.save(usuario);
        String novoToken = tokenService.gerarToken(salvo);

        return paraAuthResponse(novoToken, salvo);
    }

    @Transactional
    public void alterarSenha(AlterarSenhaDTO dto) {
        Usuario autenticado = exigirUsuarioAutenticado();
        Usuario usuario = usuarioRepository.findById(autenticado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new BadCredentialsException("A senha atual informada está incorreta");
        }

        ValidadorSenha.validarForcaEConfirmacao(dto.novaSenha(), dto.confirmacaoNovaSenha());

        usuario.atualizarSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirMinhaConta() {
        Usuario autenticado = exigirUsuarioAutenticado();
        usuarioRepository.deleteById(autenticado.getId());
    }

    private AuthResponseDTO paraAuthResponse(String token, Usuario usuario) {
        return new AuthResponseDTO(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFotoPerfil()
        );
    }

    private CadastroResponseDTO gerarEEnviarCodigo(Usuario usuario) {
        String codigo = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACAO_CODIGO);

        usuario.definirCodigoVerificacao(codigo, expiracao);
        usuarioRepository.save(usuario);

        boolean enviado = emailService.enviarCodigoVerificacao(
                usuario.getEmail(),
                usuario.getNome(),
                codigo
        );

        String mensagem = "Enviamos um código de verificação de 6 dígitos para o seu e-mail.";

        return new CadastroResponseDTO(
                mensagem,
                usuario.getEmail(),
                true,
                enviado,
                null
        );
    }

    private Usuario exigirUsuarioAutenticado() {
        Usuario usuario = obterUsuarioAutenticado();
        if (usuario == null) {
            throw new BadCredentialsException("Usuário não autenticado");
        }
        return usuario;
    }

    public static Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }
}
