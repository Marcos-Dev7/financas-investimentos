package com.marcosdev7.seguranca;

import com.marcosdev7.familia.controller.FamiliaController;
import com.marcosdev7.familia.repository.MembroFamiliaRepository;
import com.marcosdev7.familia.service.FamiliaService;
import com.marcosdev7.financas.config.ConverterStringParaEnum;
import com.marcosdev7.financas.controller.CalculoController;
import com.marcosdev7.financas.exceptions.TratamentoDeErros;
import com.marcosdev7.financas.repository.DespesaRepository;
import com.marcosdev7.financas.repository.SaldoRepository;
import com.marcosdev7.financas.service.CalculoService;
import com.marcosdev7.financas.service.DespesaService;
import com.marcosdev7.usuarios.controller.AuthController;
import com.marcosdev7.usuarios.domain.Usuario;
import com.marcosdev7.usuarios.repository.UsuarioRepository;
import com.marcosdev7.usuarios.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SegurancaSqlInjectionTest {

    @Mock
    private AuthService authService;

    @Mock
    private FamiliaService familiaService;

    @Mock
    private CalculoService calculoService;

    @Mock
    private DespesaRepository despesaRepository;

    @Test
    @DisplayName("Auditoria JPA: nenhum repositório deve usar nativeQuery ou concatenação insegura de SQL")
    void nenhumRepositorioDeveUsarNativeQueryInsegura() {
        List<Class<?>> repositorios = List.of(
                UsuarioRepository.class,
                DespesaRepository.class,
                SaldoRepository.class,
                MembroFamiliaRepository.class
        );

        for (Class<?> repo : repositorios) {
            for (Method method : repo.getDeclaredMethods()) {
                Query query = method.getAnnotation(Query.class);
                if (query != null) {
                    assertThat(query.nativeQuery())
                            .as("Repositório %s.%s não deve usar nativeQuery", repo.getSimpleName(), method.getName())
                            .isFalse();
                    assertThat(query.value())
                            .as("Consulta JPQL %s.%s deve usar parâmetros nomeados", repo.getSimpleName(), method.getName())
                            .contains(":")
                            .doesNotContain("${")
                            .doesNotContain("'%");
                }
            }
        }
    }

    @Test
    @DisplayName("Validação de entrada deve rejeitar payloads clássicos de SQL Injection em login, convite e mês")
    void deveRejeitarPayloadsDeSqlInjectionNasRotasRest() throws Exception {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new ConverterStringParaEnum());

        MockMvc authMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new TratamentoDeErros())
                .build();

        MockMvc familiaMvc = MockMvcBuilders.standaloneSetup(new FamiliaController(familiaService))
                .setControllerAdvice(new TratamentoDeErros())
                .build();

        MockMvc calculoMvc = MockMvcBuilders.standaloneSetup(new CalculoController(calculoService))
                .setControllerAdvice(new TratamentoDeErros())
                .setConversionService(conversionService)
                .build();

        // 1. Tentativa de SQLi no campo e-mail do login -> bloqueado por @Email (400)
        authMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"' OR '1'='1' --\",\"senha\":\"qualquer\"}"))
                .andExpect(status().isBadRequest());

        // 2. Tentativa de SQLi no convite familiar -> bloqueado por @Email (400)
        familiaMvc.perform(post("/familia/convites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailConvidado\":\"admin@x.com'; DROP TABLE usuario;--\"}"))
                .andExpect(status().isBadRequest());

        // 3. Tentativa de SQLi no parâmetro de mês -> rejeitado pelo conversor Enum (400)
        calculoMvc.perform(get("/calculos/sobra/janeiro' OR 1=1--"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DespesaService deve sanitizar sort malicioso (ex.: usuario.senha / SQLi) e limitar size da paginação")
    void deveSanitizarPaginacaoEOrdenacaoContraInjecao() {
        Usuario usuario = new Usuario(1L, "Marcos", "marcos@email.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );

        try {
            DespesaService despesaService = new DespesaService(despesaRepository);
            when(despesaRepository.findByUsuarioId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

            Pageable malicioso = PageRequest.of(0, 99999, Sort.by("usuario.senha", "1; DROP TABLE despesa; --"));
            despesaService.listarDespesas(malicioso);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(despesaRepository).findByUsuarioId(eq(1L), captor.capture());

            Pageable sanitizado = captor.getValue();
            assertThat(sanitizado.getPageSize()).isEqualTo(100);
            assertThat(sanitizado.getSort().getOrderFor("usuario.senha")).isNull();
            assertThat(sanitizado.getSort().getOrderFor("dataVencimento")).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
