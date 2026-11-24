package com.marcosdev7.usuarios.service;

import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.dto.*;
import com.marcosdev7.usuarios.repository.UsuarioRepository;
import com.marcosdev7.usuarios.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private TotpService totpService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar novo usuário com senha forte, gerar código de 6 dígitos e enviar e-mail")
    void deveRegistrarNovoUsuarioEEnviarCodigo() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO("Marcos Paulo", "Marcos@Email.com", "Senha@2026", "Senha@2026");

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha@2026")).thenReturn("hash_bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.enviarCodigoVerificacao(eq("marcos@email.com"), eq("Marcos Paulo"), anyString())).thenReturn(true);

        CadastroResponseDTO response = authService.registrar(dto);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo("marcos@email.com");
        assertThat(salvo.getEmailVerificado()).isFalse();
        assertThat(salvo.getCodigoVerificacao()).hasSize(6);

        assertThat(response.email()).isEqualTo("marcos@email.com");
        assertThat(response.requerVerificacao()).isTrue();
        assertThat(response.emailEnviadoViaSmtp()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar cadastro quando as senhas não coincidirem")
    void deveRejeitarCadastroComConfirmacaoDiferente() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO("Marcos", "marcos@email.com", "Senha@2026", "Diferente@2026");

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não coincide");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar cadastro quando e-mail já estiver verificado")
    void deveRejeitarEmailJaVerificado() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO("Marcos", "marcos@email.com", "Senha@2026", "Senha@2026");
        Usuario existente = new Usuario(1L, "Marcos", "marcos@email.com", "hash");
        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este e-mail já está cadastrado");
    }

    @Test
    @DisplayName("Deve confirmar e-mail com código válido e retornar token JWT")
    void deveConfirmarEmailComCodigoValido() {
        Usuario usuario = new Usuario("Marcos Paulo", "marcos@email.com", "hash_bcrypt");
        usuario.definirCodigoVerificacao("123456", LocalDateTime.now().plusMinutes(15));

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(usuario));
        when(tokenService.gerarToken(usuario)).thenReturn("jwt_token_valido");

        AuthResponseDTO response = authService.verificarEmail(new VerificarEmailDTO("marcos@email.com", "123456"));

        assertThat(usuario.getEmailVerificado()).isTrue();
        assertThat(usuario.getCodigoVerificacao()).isNull();
        assertThat(response.token()).isEqualTo("jwt_token_valido");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Deve realizar login com sucesso para usuário com e-mail verificado")
    void deveRealizarLoginComSucesso() {
        LoginRequestDTO dto = new LoginRequestDTO("marcos@email.com", "Senha@2026");
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_bcrypt");

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@2026", "hash_bcrypt")).thenReturn(true);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt_token_valido");

        AuthResponseDTO response = authService.login(dto);

        assertThat(response.token()).isEqualTo("jwt_token_valido");
        assertThat(response.email()).isEqualTo("marcos@email.com");
    }

    @Test
    @DisplayName("Deve bloquear login se o e-mail ainda não tiver sido verificado")
    void deveBloquearLoginQuandoEmailNaoVerificado() {
        LoginRequestDTO dto = new LoginRequestDTO("marcos@email.com", "Senha@2026");
        Usuario usuario = new Usuario("Marcos Paulo", "marcos@email.com", "hash_bcrypt");

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@2026", "hash_bcrypt")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EMAIL_NAO_VERIFICADO:marcos@email.com");
    }

    @Test
    @DisplayName("Deve gerar token e enviar link de recuperação de senha via e-mail de recuperação")
    void deveEnviarLinkRecuperacaoPorEmailRecuperacao() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash");
        usuario.atualizarEmailRecuperacao("backup@email.com");

        when(usuarioRepository.findByEmailRecuperacaoIgnoreCase("backup@email.com"))
                .thenReturn(Optional.of(usuario));
        when(emailService.enviarLinkRecuperacaoSenha(eq("backup@email.com"), eq("Marcos Paulo"), anyString(), anyString()))
                .thenReturn(true);

        EsqueciSenhaResponseDTO response = authService.solicitarRecuperacaoSenha(
                new EsqueciSenhaRequestDTO("EMAIL_RECUPERACAO", "backup@email.com"),
                "https://financial-family.onrender.com"
        );

        assertThat(response.emailEnviado()).isTrue();
        assertThat(usuario.getTokenRecuperacao()).isNotBlank();
        assertThat(usuario.getTokenRecuperacaoExpiracao()).isAfter(LocalDateTime.now());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Deve localizar conta pelo telefone, gerar código de 6 dígitos e enviar via SMS quando disponível")
    void deveEnviarCodigo6DigitosViaSmsPorTelefone() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash");
        usuario.atualizarTelefone("(11) 99999-8888");
        usuario.atualizarEmailRecuperacao("rec@email.com");

        when(usuarioRepository.findByTelefone("11999998888")).thenReturn(Optional.of(usuario));
        when(emailService.enviarSmsCodigoRecuperacao(eq("11999998888"), matches("\\d{6}")))
                .thenReturn(true);

        EsqueciSenhaResponseDTO response = authService.solicitarRecuperacaoSenha(
                new EsqueciSenhaRequestDTO("TELEFONE", "(11) 99999-8888"),
                "https://financial-family.onrender.com"
        );

        assertThat(response.smsEnviado()).isTrue();
        assertThat(usuario.getTokenRecuperacao()).matches("\\d{6}");
        verify(emailService, never()).enviarLinkRecuperacaoSenha(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve fazer fallback para o e-mail vinculado com o código de 6 dígitos se o SMS não estiver disponível")
    void deveFazerFallbackParaEmailQuandoSmsIndisponivel() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash");
        usuario.atualizarTelefone("(11) 99999-8888");
        usuario.atualizarEmailRecuperacao("rec@email.com");

        when(usuarioRepository.findByTelefone("11999998888")).thenReturn(Optional.of(usuario));
        when(emailService.enviarSmsCodigoRecuperacao(eq("11999998888"), matches("\\d{6}")))
                .thenReturn(false);
        when(emailService.enviarLinkRecuperacaoSenha(eq("rec@email.com"), eq("Marcos Paulo"), contains("?resetToken="), matches("\\d{6}")))
                .thenReturn(true);

        EsqueciSenhaResponseDTO response = authService.solicitarRecuperacaoSenha(
                new EsqueciSenhaRequestDTO("TELEFONE", "(11) 99999-8888"),
                "https://financial-family.onrender.com"
        );

        assertThat(response.smsEnviado()).isFalse();
        assertThat(response.emailEnviado()).isTrue();
        assertThat(usuario.getTokenRecuperacao()).matches("\\d{6}");
    }

    @Test
    @DisplayName("Deve redefinir a senha com token válido e limpar o token após o uso")
    void deveRedefinirSenhaComTokenValido() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_antigo");
        usuario.definirTokenRecuperacao("token-valido-123", LocalDateTime.now().plusMinutes(20));

        when(usuarioRepository.findByTokenRecuperacao("token-valido-123")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("hash_novo");

        authService.redefinirSenha(new RedefinirSenhaRequestDTO("token-valido-123", "NovaSenha@2026", "NovaSenha@2026"));

        assertThat(usuario.getSenha()).isEqualTo("hash_novo");
        assertThat(usuario.getTokenRecuperacao()).isNull();
        assertThat(usuario.getTokenRecuperacaoExpiracao()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Deve recusar redefinição de senha quando o token estiver expirado")
    void deveRecusarRedefinicaoQuandoTokenExpirado() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_antigo");
        usuario.definirTokenRecuperacao("token-expirado", LocalDateTime.now().minusMinutes(5));

        when(usuarioRepository.findByTokenRecuperacao("token-expirado")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.redefinirSenha(
                new RedefinirSenhaRequestDTO("token-expirado", "NovaSenha@2026", "NovaSenha@2026")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    @DisplayName("Login com 2FA via E-mail não deve emitir JWT antes da verificação do código de 6 dígitos")
    void deveExigir2FaViaEmailNoLoginEEmitirJwtAposValidarCodigo() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_bcrypt");
        usuario.ativar2FaEmail();

        when(usuarioRepository.findByEmail("marcos@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@2026", "hash_bcrypt")).thenReturn(true);
        when(emailService.enviarCodigo2Fa(eq("marcos@email.com"), eq("Marcos Paulo"), matches("\\d{6}")))
                .thenReturn(true);

        AuthResponseDTO desafio = authService.login(new LoginRequestDTO("marcos@email.com", "Senha@2026"));

        assertThat(desafio.token()).isNull();
        assertThat(desafio.requerDoisFatores()).isTrue();
        assertThat(desafio.tipoDoisFatores()).isEqualTo("EMAIL");
        assertThat(desafio.desafioToken()).isNotBlank();
        verify(tokenService, never()).gerarToken(any());

        // Etapa 2: Validar código de 6 dígitos recebido
        String codigoGerado = usuario.getCodigo2Fa();
        when(usuarioRepository.findByDesafio2FaToken(desafio.desafioToken())).thenReturn(Optional.of(usuario));
        when(tokenService.gerarToken(usuario)).thenReturn("jwt_pos_2fa");

        AuthResponseDTO autenticado = authService.verificarDoisFatores(
                new Verificar2FaDTO(desafio.desafioToken(), codigoGerado)
        );

        assertThat(autenticado.token()).isEqualTo("jwt_pos_2fa");
        assertThat(autenticado.requerDoisFatores()).isFalse();
        assertThat(usuario.getDesafio2FaToken()).isNull();
    }

    @Test
    @DisplayName("Deve bloquear e invalidar o desafio 2FA após 5 tentativas incorretas")
    void deveBloquearDesafio2FaAposCincoTentativasIncorretas() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_bcrypt");
        usuario.ativar2FaEmail();
        usuario.iniciarDesafio2Fa("desafio-abc", "654321", LocalDateTime.now().plusMinutes(5));

        when(usuarioRepository.findByDesafio2FaToken("desafio-abc")).thenReturn(Optional.of(usuario));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.verificarDoisFatores(new Verificar2FaDTO("desafio-abc", "000000")))
                    .isInstanceOf(BadCredentialsException.class);
            assertThat(usuario.getDesafio2FaToken()).isEqualTo("desafio-abc");
        }

        // 5ª tentativa incorreta invalida o desafio imediatamente
        assertThatThrownBy(() -> authService.verificarDoisFatores(new Verificar2FaDTO("desafio-abc", "000000")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Muitas tentativas");
        assertThat(usuario.getDesafio2FaToken()).isNull();
    }

    @Test
    @DisplayName("Login com 2FA via App Autenticador (TOTP) deve validar código via TotpService")
    void deveValidar2FaViaAppAutenticadorTotp() {
        Usuario usuario = new Usuario(1L, "Marcos Paulo", "marcos@email.com", "hash_bcrypt");
        usuario.prepararTotpPendente("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP");
        usuario.confirmarTotp();
        usuario.iniciarDesafio2Fa("desafio-totp", null, LocalDateTime.now().plusMinutes(5));

        when(usuarioRepository.findByDesafio2FaToken("desafio-totp")).thenReturn(Optional.of(usuario));
        when(totpService.validarCodigo("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt_totp_ok");

        AuthResponseDTO autenticado = authService.verificarDoisFatores(new Verificar2FaDTO("desafio-totp", "123456"));

        assertThat(autenticado.token()).isEqualTo("jwt_totp_ok");
        assertThat(usuario.getDesafio2FaToken()).isNull();
    }
}
