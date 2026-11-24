package com.marcosdev7.usuarios.controller;

import com.marcosdev7.financas.dto.DadosErroMensagemDTO;
import com.marcosdev7.usuarios.dto.*;
import com.marcosdev7.usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticação & Perfil", description = "Cadastro com validação de e-mail, login JWT e gestão de perfil")
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Cadastra um novo usuário e envia código por e-mail",
            description = "Valida força da senha e confirmação, salva com BCrypt e envia código de 6 dígitos para confirmação.")
    @PostMapping("/registrar")
    public ResponseEntity<CadastroResponseDTO> registrar(@RequestBody @Valid CadastroUsuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    @Operation(summary = "Valida o código de 6 dígitos enviado por e-mail",
            description = "Confirma o e-mail do usuário e já retorna o token JWT de sessão.")
    @PostMapping("/verificar-email")
    public ResponseEntity<AuthResponseDTO> verificarEmail(@RequestBody @Valid VerificarEmailDTO dto) {
        return ResponseEntity.ok(authService.verificarEmail(dto));
    }

    @Operation(summary = "Reenvia o código de verificação para o e-mail",
            description = "Gera um novo código de 6 dígitos válido por 15 minutos e envia ao e-mail do usuário.")
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<CadastroResponseDTO> reenviarCodigo(@RequestBody @Valid ReenviarCodigoDTO dto) {
        return ResponseEntity.ok(authService.reenviarCodigo(dto));
    }

    @Operation(summary = "Realiza login na plataforma",
            description = "Autentica e-mail e senha (exige e-mail verificado). Se 2FA estiver ativo, retorna desafio 2FA sem emitir JWT.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "Valida o código de 6 dígitos da verificação em 2 fatores (2FA)",
            description = "Conclui o login após validar o código TOTP (App) ou código enviado por e-mail e retorna o token JWT.")
    @PostMapping("/verificar-2fa")
    public ResponseEntity<AuthResponseDTO> verificarDoisFatores(@RequestBody @Valid Verificar2FaDTO dto) {
        return ResponseEntity.ok(authService.verificarDoisFatores(dto));
    }

    @Operation(summary = "Ativa 2FA via E-mail para a conta logada",
            description = "Exige confirmação da senha atual e passa a exigir código de 6 dígitos por e-mail a cada login.")
    @PostMapping("/2fa/ativar-email")
    public ResponseEntity<UsuarioResponseDTO> ativar2FaEmail(@RequestBody @Valid Configurar2FaSenhaDTO dto) {
        return ResponseEntity.ok(authService.ativar2FaEmail(dto));
    }

    @Operation(summary = "Inicia configuração de 2FA via App Autenticador (TOTP)",
            description = "Gera uma chave Base32 (RFC 6238) e URI otpauth:// para cadastro no Google Authenticator, Authy ou Apple Senhas.")
    @PostMapping("/2fa/totp/iniciar")
    public ResponseEntity<IniciarTotpResponseDTO> iniciarTotp() {
        return ResponseEntity.ok(authService.iniciarConfiguracaoTotp());
    }

    @Operation(summary = "Confirma e ativa 2FA via App Autenticador (TOTP)",
            description = "Valida senha atual e o código de 6 dígitos do app autenticador para concluir a ativação do 2FA.")
    @PostMapping("/2fa/totp/confirmar")
    public ResponseEntity<UsuarioResponseDTO> confirmarTotp(@RequestBody @Valid ConfirmarTotpDTO dto) {
        return ResponseEntity.ok(authService.confirmarConfiguracaoTotp(dto));
    }

    @Operation(summary = "Desativa a verificação em 2 fatores (2FA)",
            description = "Exige confirmação da senha atual e remove o segundo fator da conta.")
    @PostMapping("/2fa/desativar")
    public ResponseEntity<UsuarioResponseDTO> desativar2Fa(@RequestBody @Valid Configurar2FaSenhaDTO dto) {
        return ResponseEntity.ok(authService.desativar2Fa(dto));
    }

    @Operation(summary = "Solicita link de recuperação de senha",
            description = "Localiza a conta por e-mail principal, e-mail de recuperação ou telefone e envia link de redefinição.")
    @PostMapping("/esqueci-senha")
    public ResponseEntity<EsqueciSenhaResponseDTO> esqueciSenha(
            @RequestBody @Valid EsqueciSenhaRequestDTO dto,
            HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            String proto = request.getHeader("X-Forwarded-Proto");
            String scheme = (proto != null && !proto.isBlank()) ? proto : request.getScheme();
            String host = request.getHeader("Host");
            origin = (host != null && !host.isBlank())
                    ? scheme + "://" + host
                    : scheme + "://" + request.getServerName() + ":" + request.getServerPort();
        }
        return ResponseEntity.ok(authService.solicitarRecuperacaoSenha(dto, origin));
    }

    @Operation(summary = "Redefine a senha com token de recuperação",
            description = "Valida o token enviado no link de recuperação e atualiza a senha do usuário.")
    @PostMapping("/redefinir-senha")
    public ResponseEntity<DadosErroMensagemDTO> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequestDTO dto) {
        authService.redefinirSenha(dto);
        return ResponseEntity.ok(new DadosErroMensagemDTO("Senha redefinida com sucesso. Já pode fazer login."));
    }

    @Operation(summary = "Dados do perfil autenticado",
            description = "Retorna os dados do usuário logado.")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(authService.obterPerfilAtual());
    }

    @Operation(summary = "Atualiza os dados da conta do usuário logado",
            description = "Permite alterar nome e e-mail do usuário autenticado e retorna um novo token JWT atualizado.")
    @PutMapping("/perfil")
    public ResponseEntity<AuthResponseDTO> atualizarPerfil(@RequestBody @Valid AtualizarPerfilDTO dto) {
        return ResponseEntity.ok(authService.atualizarPerfil(dto));
    }

    @Operation(summary = "Altera a senha do usuário logado",
            description = "Valida a senha atual, aplica as regras de senha forte + confirmação e atualiza o hash BCrypt.")
    @PutMapping("/alterar-senha")
    public ResponseEntity<DadosErroMensagemDTO> alterarSenha(@RequestBody @Valid AlterarSenhaDTO dto) {
        authService.alterarSenha(dto);
        return ResponseEntity.ok(new DadosErroMensagemDTO("Senha alterada com sucesso"));
    }

    @Operation(summary = "Exclui a conta do usuário logado",
            description = "Remove permanentemente a conta do usuário e seus lançamentos vinculados.")
    @DeleteMapping("/conta")
    public ResponseEntity<Void> excluirConta() {
        authService.excluirMinhaConta();
        return ResponseEntity.noContent().build();
    }
}
