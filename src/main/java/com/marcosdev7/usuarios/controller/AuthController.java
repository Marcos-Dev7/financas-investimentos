package com.marcosdev7.usuarios.controller;

import com.marcosdev7.financas.dto.DadosErroMensagemDTO;
import com.marcosdev7.usuarios.dto.*;
import com.marcosdev7.usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            description = "Autentica e-mail e senha (exige e-mail verificado) e retorna o token JWT Bearer.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
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
