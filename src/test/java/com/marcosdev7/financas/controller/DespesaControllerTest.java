package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.dto.DespesaRequestDTO;
import com.marcosdev7.financas.dto.DespesaResponseDTO;
import com.marcosdev7.financas.exceptions.TratamentoDeErros;
import com.marcosdev7.financas.service.DespesaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DespesaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DespesaService despesaService;

    @InjectMocks
    private DespesaController despesaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(despesaController)
                .setControllerAdvice(new TratamentoDeErros())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /despesas - Deve retornar página de despesas com status 200")
    void deveListarDespesasComStatus200() throws Exception {
        DespesaResponseDTO dto = new DespesaResponseDTO(
                1L,
                "Internet",
                new BigDecimal("120.00"),
                Mes.MAIO,
                LocalDate.of(2026, 5, 10),
                true,
                "Moradia",
                false
        );

        when(despesaService.listarDespesas(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/despesas")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Internet"))
                .andExpect(jsonPath("$.content[0].mes").value("maio"))
                .andExpect(jsonPath("$.content[0].dataVencimento").value("10/05/2026"))
                .andExpect(jsonPath("$.content[0].isPaga").value(false));
    }

    @Test
    @DisplayName("POST /despesas - Deve cadastrar despesa válida e retornar status 201")
    void deveCadastrarDespesaComStatus201() throws Exception {
        String json = """
                {
                    "descricao": "Conta de Energia",
                    "valor": 210.50,
                    "mes": "maio",
                    "dataVencimento": "15/05/2026",
                    "isFixa": true,
                    "categoria": "Moradia"
                }
                """;

        mockMvc.perform(post("/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(despesaService).cadastrarDespesas(any(DespesaRequestDTO.class));
    }

    @Test
    @DisplayName("POST /despesas - Deve retornar 400 quando dados obrigatórios forem inválidos")
    void deveRetornar400QuandoDespesaInvalida() throws Exception {
        String jsonInvalido = """
                {
                    "descricao": "Oi",
                    "valor": -10.00
                }
                """;

        mockMvc.perform(post("/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isArray());

        verify(despesaService, never()).cadastrarDespesas(any());
    }

    @Test
    @DisplayName("POST /despesas - Deve retornar 400 quando o corpo da requisição estiver ausente")
    void deveRetornar400QuandoCorpoRequisicaoAusente() throws Exception {
        mockMvc.perform(post("/despesas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("O corpo da requisição e obrigatório"));
    }

    @Test
    @DisplayName("PUT /despesas/{id} - Deve atualizar despesa e retornar status 200")
    void deveAtualizarDespesaComStatus200() throws Exception {
        DespesaResponseDTO atualizada = new DespesaResponseDTO(
                1L,
                "Aluguel Atualizado",
                new BigDecimal("1600.00"),
                Mes.MAIO,
                LocalDate.of(2026, 5, 10),
                true,
                "Moradia",
                false
        );

        when(despesaService.atualizarDespesas(eq(1L), any(DespesaRequestDTO.class))).thenReturn(atualizada);

        String json = """
                {
                    "descricao": "Aluguel Atualizado",
                    "valor": 1600.00,
                    "mes": "maio",
                    "dataVencimento": "10/05/2026",
                    "isFixa": true,
                    "categoria": "Moradia"
                }
                """;

        mockMvc.perform(put("/despesas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao").value("Aluguel Atualizado"))
                .andExpect(jsonPath("$.valor").value(1600.00));
    }

    @Test
    @DisplayName("PUT /despesas/{id} - Deve retornar 400 quando valor for negativo ou nulo")
    void deveRetornar400AoAtualizarDespesaComValorInvalido() throws Exception {
        String jsonInvalido = """
                {
                    "descricao": "Aluguel Negativo",
                    "valor": -500.00,
                    "mes": "maio",
                    "dataVencimento": "10/05/2026"
                }
                """;

        mockMvc.perform(put("/despesas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isArray());

        verify(despesaService, never()).atualizarDespesas(any(), any());
    }

    @Test
    @DisplayName("PATCH /despesas/{id}/pagar - Deve marcar despesa como paga e retornar 200")
    void devePagarDespesaComStatus200() throws Exception {
        DespesaResponseDTO paga = new DespesaResponseDTO(
                1L, "Luz", new BigDecimal("150.00"), Mes.MAIO,
                LocalDate.of(2026, 5, 10), true, "Moradia", true
        );
        when(despesaService.pagarDespesa(1L)).thenReturn(paga);

        mockMvc.perform(patch("/despesas/1/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaga").value(true));
    }

    @Test
    @DisplayName("PATCH /despesas/{id}/estornar - Deve estornar despesa e retornar 200")
    void deveEstornarDespesaComStatus200() throws Exception {
        DespesaResponseDTO estornada = new DespesaResponseDTO(
                1L, "Luz", new BigDecimal("150.00"), Mes.MAIO,
                LocalDate.of(2026, 5, 10), true, "Moradia", false
        );
        when(despesaService.estornarDespesa(1L)).thenReturn(estornada);

        mockMvc.perform(patch("/despesas/1/estornar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaga").value(false));
    }

    @Test
    @DisplayName("DELETE /despesas/{id} - Deve remover despesa e retornar status 204")
    void deveDeletarDespesaComStatus204() throws Exception {
        mockMvc.perform(delete("/despesas/1"))
                .andExpect(status().isNoContent());

        verify(despesaService).deletarDespesas(1L);
    }

    @Test
    @DisplayName("DELETE /despesas/{id} - Deve retornar 404 quando ID não existir")
    void deveRetornar404QuandoIdNaoExistir() throws Exception {
        doThrow(new NoSuchElementException()).when(despesaService).deletarDespesas(99L);

        mockMvc.perform(delete("/despesas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("ID não encontrado"));
    }
}
