package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.SaldoRequestDTO;
import com.marcosdev7.financas.dto.SaldoResponseDTO;
import com.marcosdev7.financas.exceptions.TratamentoDeErros;
import com.marcosdev7.financas.service.SaldoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SaldoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SaldoService saldoService;

    @InjectMocks
    private SaldoController saldoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(saldoController)
                .setControllerAdvice(new TratamentoDeErros())
                .build();
    }

    @Test
    @DisplayName("GET /saldos - Deve listar todos os saldos com status 200")
    void deveListarSaldosComStatus200() throws Exception {
        SaldoResponseDTO dto = new SaldoResponseDTO(1L, "Salário", new BigDecimal("5000.00"), Mes.MAIO, 2026);
        when(saldoService.listarSaldos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/saldos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Salário"))
                .andExpect(jsonPath("$[0].valor").value(5000.00))
                .andExpect(jsonPath("$[0].mes").value("maio"))
                .andExpect(jsonPath("$[0].ano").value(2026));
    }

    @Test
    @DisplayName("POST /saldos - Deve cadastrar novo saldo válido e retornar status 201")
    void deveCadastrarSaldoComStatus201() throws Exception {
        String json = """
                {
                    "descricao": "Salário Mensal",
                    "valor": 5500.00,
                    "mes": "maio",
                    "ano": 2026
                }
                """;

        mockMvc.perform(post("/saldos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(saldoService).cadastrarSaldo(any(SaldoRequestDTO.class));
    }

    @Test
    @DisplayName("POST /saldos - Deve retornar 400 quando dados do saldo forem inválidos")
    void deveRetornar400QuandoSaldoInvalido() throws Exception {
        String jsonInvalido = """
                {
                    "descricao": "AB",
                    "valor": -100.00
                }
                """;

        mockMvc.perform(post("/saldos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isArray());

        verify(saldoService, never()).cadastrarSaldo(any());
    }

    @Test
    @DisplayName("PUT /saldos/{id} - Deve atualizar saldo existente e retornar status 200")
    void deveAtualizarSaldoComStatus200() throws Exception {
        SaldoResponseDTO atualizado = new SaldoResponseDTO(1L, "Salário Reajustado", new BigDecimal("6000.00"), Mes.MAIO, 2026);
        when(saldoService.atualizarSaldos(eq(1L), any(SaldoRequestDTO.class))).thenReturn(atualizado);

        String json = """
                {
                    "descricao": "Salário Reajustado",
                    "valor": 6000.00,
                    "mes": "maio",
                    "ano": 2026
                }
                """;

        mockMvc.perform(put("/saldos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao").value("Salário Reajustado"))
                .andExpect(jsonPath("$.valor").value(6000.00));
    }

    @Test
    @DisplayName("PUT /saldos/{id} - Deve retornar 400 quando valor for negativo ou ano fora do intervalo")
    void deveRetornar400AoAtualizarSaldoComValorNegativoOuAnoInvalido() throws Exception {
        String jsonInvalido = """
                {
                    "descricao": "Salário Fraudado",
                    "valor": -1000.00,
                    "mes": "maio",
                    "ano": 1800
                }
                """;

        mockMvc.perform(put("/saldos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isArray());

        verify(saldoService, never()).atualizarSaldos(any(), any());
    }

    @Test
    @DisplayName("DELETE /saldos/{id} - Deve apagar saldo existente e retornar status 204")
    void deveDeletarSaldoComStatus204() throws Exception {
        mockMvc.perform(delete("/saldos/1"))
                .andExpect(status().isNoContent());

        verify(saldoService).deletarSaldos(1L);
    }

    @Test
    @DisplayName("DELETE /saldos/{id} - Deve retornar 404 quando saldo não existir")
    void deveRetornar404AoDeletarSaldoInexistente() throws Exception {
        doThrow(new NoSuchElementException()).when(saldoService).deletarSaldos(99L);

        mockMvc.perform(delete("/saldos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("ID não encontrado"));
    }
}
