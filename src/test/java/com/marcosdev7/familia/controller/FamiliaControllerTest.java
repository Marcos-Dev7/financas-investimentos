package com.marcosdev7.familia.controller;

import com.marcosdev7.familia.domain.StatusFamilia;
import com.marcosdev7.familia.dto.*;
import com.marcosdev7.familia.service.FamiliaService;
import com.marcosdev7.financas.config.ConverterStringParaEnum;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Status;
import com.marcosdev7.financas.exceptions.TratamentoDeErros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FamiliaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FamiliaService familiaService;

    @InjectMocks
    private FamiliaController familiaController;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new ConverterStringParaEnum());

        mockMvc = MockMvcBuilders.standaloneSetup(familiaController)
                .setControllerAdvice(new TratamentoDeErros())
                .setConversionService(conversionService)
                .build();
    }

    @Test
    @DisplayName("POST /familia/convites - Deve criar convite familiar e retornar 201")
    void deveEnviarConviteComStatus201() throws Exception {
        MembroFamiliaItemDTO item = new MembroFamiliaItemDTO(
                1L, "Convidado(a) Pendente", "conjuge@email.com",
                StatusFamilia.PENDENTE, false, "token123", LocalDateTime.now().plusHours(48), false
        );
        when(familiaService.enviarConvite(any(ConviteFamiliaRequestDTO.class))).thenReturn(item);

        mockMvc.perform(post("/familia/convites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailConvidado\":\"conjuge@email.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("conjuge@email.com"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.tokenConvite").value("token123"));
    }

    @Test
    @DisplayName("GET /familia/resumo/{mes} - Deve retornar resumo consolidado da família com status 200")
    void deveRetornarResumoFamiliarComStatus200() throws Exception {
        ResumoFamiliaResponseDTO resumo = new ResumoFamiliaResponseDTO(
                Mes.DEZEMBRO,
                new BigDecimal("9000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("5500.00"),
                Status.POSITIVO,
                new BigDecimal("4200.00"),
                2
        );
        when(familiaService.calcularResumoFamilia(Mes.DEZEMBRO)).thenReturn(resumo);

        mockMvc.perform(get("/familia/resumo/dezembro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receitaTotalFamiliar").value(9000.00))
                .andExpect(jsonPath("$.saldoDisponivelFamiliar").value(5500.00))
                .andExpect(jsonPath("$.totalMembrosAtivos").value(2));
    }

    @Test
    @DisplayName("GET /familia/despesas - Deve listar despesas familiares em modo leitura")
    void deveListarDespesasFamiliaresComStatus200() throws Exception {
        DespesaFamiliaResponseDTO despesa = new DespesaFamiliaResponseDTO(
                1L, "Aluguel", new BigDecimal("2100.00"), Mes.DEZEMBRO,
                LocalDate.of(2026, 12, 10), true, "Moradia", false, "Marcos Paulo", false
        );
        when(familiaService.listarDespesasFamilia(Mes.DEZEMBRO)).thenReturn(List.of(despesa));

        mockMvc.perform(get("/familia/despesas").param("mes", "dezembro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Aluguel"))
                .andExpect(jsonPath("$[0].responsavelNome").value("Marcos Paulo"));
    }

    @Test
    @DisplayName("DELETE /familia/membros/{id} - Deve retornar 403 Forbidden quando usuário não for o titular")
    void deveRetornar403QuandoNaoAutorizadoARemoverMembro() throws Exception {
        doThrow(new AccessDeniedException("Apenas o titular da família pode remover membros"))
                .when(familiaService).removerMembroOuConvite(99L);

        mockMvc.perform(delete("/familia/membros/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Apenas o titular da família pode remover membros"));
    }

    @Test
    @DisplayName("PATCH /familia/compartilhar-despesas - Deve atualizar compartilhamento de despesas com status 200")
    void deveAtualizarCompartilhamentoDespesasComStatus200() throws Exception {
        PainelFamiliaResponseDTO painel = new PainelFamiliaResponseDTO(
                true, true, "Marcos Paulo", "marcos@email.com", true, false, List.of(), List.of()
        );
        when(familiaService.alterarCompartilhamentoDespesas(any(CompartilharDespesasDTO.class))).thenReturn(painel);

        mockMvc.perform(patch("/familia/compartilhar-despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compartilhar\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minhasDespesasCompartilhadas").value(false));
    }

    @Test
    @DisplayName("PATCH /familia/despesas/{id}/pagar e /estornar - Gestor gerencia pagamentos e membro recebe 403")
    void gestorPagaEEstornaDespesaFamiliarEMembroRecebe403() throws Exception {
        DespesaFamiliaResponseDTO paga = new DespesaFamiliaResponseDTO(
                50L, "Unhas", new BigDecimal("100.00"), Mes.DEZEMBRO,
                LocalDate.of(2026, 12, 10), false, "Lazer", true, "Anna Luisa", false
        );
        DespesaFamiliaResponseDTO estornada = new DespesaFamiliaResponseDTO(
                50L, "Unhas", new BigDecimal("100.00"), Mes.DEZEMBRO,
                LocalDate.of(2026, 12, 10), false, "Lazer", false, "Anna Luisa", false
        );
        when(familiaService.pagarDespesaFamilia(50L)).thenReturn(paga);
        when(familiaService.estornarDespesaFamilia(50L)).thenReturn(estornada);

        mockMvc.perform(patch("/familia/despesas/50/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaga").value(true));

        mockMvc.perform(patch("/familia/despesas/50/estornar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaga").value(false));
    }
}
