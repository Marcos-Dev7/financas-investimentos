package com.marcosdev7.usuarios.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RateLimitFilterTest {

    @Test
    @DisplayName("Deve permitir requisições dentro do limite e bloquear com HTTP 429 ao exceder o limite de autenticação")
    void deveBloquearQuandoExcederLimiteDeAutenticacao() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("192.168.1.50");
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilter(req, res, chain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 16ª tentativa no mesmo minuto deve retornar 429 Too Many Requests
        MockHttpServletRequest reqExcedida = new MockHttpServletRequest("POST", "/auth/login");
        reqExcedida.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse resExcedida = new MockHttpServletResponse();

        filter.doFilter(reqExcedida, resExcedida, chain);
        assertThat(resExcedida.getStatus()).isEqualTo(429);
        assertThat(resExcedida.getContentAsString()).contains("Muitas requisições");
    }
}
