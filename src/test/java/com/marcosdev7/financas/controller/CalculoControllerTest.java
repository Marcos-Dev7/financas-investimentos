package com.marcosdev7.financas.controller;

import com.marcosdev7.financas.config.ConverterStringParaEnum;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.domain.Status;
import com.marcosdev7.financas.dto.SobraMesResponseDTO;
import com.marcosdev7.financas.exceptions.TratamentoDeErros;
import com.marcosdev7.financas.service.CalculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CalculoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CalculoService calculoService;

    @InjectMocks
    private CalculoController calculoController;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new ConverterStringParaEnum());

        mockMvc = MockMvcBuilders.standaloneSetup(calculoController)
                .setControllerAdvice(new TratamentoDeErros())
                .setConversionService(conversionService)
                .build();
    }

    @Test
    @DisplayName("GET /calculos/sobra/{mes} - Deve retornar o resumo de sobra livre do mês (case-insensitive)")
    void deveRetornarSobraLivreDoMesComStatus200() throws Exception {
        SobraMesResponseDTO dto = new SobraMesResponseDTO(
                Mes.MAIO,
                new BigDecimal("6000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("2500.00"),
                Status.POSITIVO,
                new BigDecimal("4000.00")
        );

        when(calculoService.calculaSobraLivreMes(Mes.MAIO)).thenReturn(dto);

        mockMvc.perform(get("/calculos/sobra/maio")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("maio"))
                .andExpect(jsonPath("$.receitaTotal").value(6000.00))
                .andExpect(jsonPath("$.despesasPagas").value(3500.00))
                .andExpect(jsonPath("$.saldoDisponivel").value(2500.00))
                .andExpect(jsonPath("$.status").value("POSITIVO"))
                .andExpect(jsonPath("$.despesasBrutas").value(4000.00));
    }
}
