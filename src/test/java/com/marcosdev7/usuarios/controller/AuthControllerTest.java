package com.marcosdev7.usuarios.controller;

import com.marcosdev7.financas.exceptions.TratamentoDeErros;
import com.marcosdev7.usuarios.dto.*;
import com.marcosdev7.usuarios.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new TratamentoDeErros())
                .build();
    }

    @Test
    @DisplayName("POST /auth/registrar - Deve cadastrar usuário e retornar 201 com status de verificação")
    void deveRegistrarUsuarioComStatus201() throws Exception {
        CadastroResponseDTO response = new CadastroResponseDTO(
                "Enviamos um código de 6 dígitos para o seu e-mail.",
                "marcos@email.com",
                true,
                true,
                null
        );
        when(authService.registrar(any(CadastroUsuarioDTO.class))).thenReturn(response);

        String json = """
                {
                    "nome": "Marcos Paulo",
                    "email": "marcos@email.com",
                    "senha": "Password@123",
                    "confirmacaoSenha": "Password@123"
                }
                """;

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("marcos@email.com"))
                .andExpect(jsonPath("$.requerVerificacao").value(true));
    }

    @Test
    @DisplayName("POST /auth/verificar-email - Deve validar código e retornar 200 com token JWT")
    void deveVerificarEmailComStatus200() throws Exception {
        AuthResponseDTO response = new AuthResponseDTO("jwt_verificado", 1L, "Marcos Paulo", "marcos@email.com");
        when(authService.verificarEmail(any(VerificarEmailDTO.class))).thenReturn(response);

        String json = """
                {
                    "email": "marcos@email.com",
                    "codigo": "123456"
                }
                """;

        mockMvc.perform(post("/auth/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_verificado"));
    }

    @Test
    @DisplayName("POST /auth/login - Deve realizar login e retornar 200 com token")
    void deveFazerLoginComStatus200() throws Exception {
        AuthResponseDTO response = new AuthResponseDTO("jwt_abc", 1L, "Marcos Paulo", "marcos@email.com");
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        String json = """
                {
                    "email": "marcos@email.com",
                    "senha": "Password@123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_abc"));
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 401 quando credenciais forem inválidas")
    void deveRetornar401QuandoLoginInvalido() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("E-mail ou senha inválidos"));

        String json = """
                {
                    "email": "marcos@email.com",
                    "senha": "errada"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }
}
