package com.marcosdev7.usuarios.controller;

import com.marcosdev7.usuarios.dto.AuthResponseDTO;
import com.marcosdev7.usuarios.dto.CadastroUsuarioDTO;
import com.marcosdev7.usuarios.dto.LoginRequestDTO;
import com.marcosdev7.usuarios.dto.UsuarioResponseDTO;
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
@Tag(name = "Autenticação & Usuários", description = "Cadastro de conta, login JWT e dados da sessão")
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Cadastra um novo usuário",
            description = "Cria uma nova conta com senha criptografada (BCrypt) e já retorna o token JWT de acesso.")
    @PostMapping("/registrar")
    public ResponseEntity<AuthResponseDTO> registrar(@RequestBody @Valid CadastroUsuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    @Operation(summary = "Realiza login na plataforma",
            description = "Autentica e-mail e senha e retorna o token JWT Bearer válido por 24h.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "Dados do usuário autenticado",
            description = "Retorna o perfil do usuário correspondente ao token JWT enviado no header Authorization.")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(authService.obterPerfilAtual());
    }
}
