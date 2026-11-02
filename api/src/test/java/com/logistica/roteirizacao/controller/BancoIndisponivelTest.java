package com.logistica.roteirizacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.logistica.roteirizacao.repository.RotaCalculadaRepository;

/** Simula a queda do banco: o ControllerAdvice deve responder 503 no formato padrão. */
class BancoIndisponivelTest extends ApiIntegracaoTeste {

    @MockitoBean
    private RotaCalculadaRepository rotaRepository;

    @Test
    void falhaDeConexaoComOBancoGera503() throws Exception {
        when(rotaRepository.findById(any())).thenThrow(new DataAccessResourceFailureException("Conexão recusada"));

        mvc.perform(get("/rotas/{id}", 1))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.erro").value("INDISPONIVEL"))
                .andExpect(jsonPath("$.mensagem").value("Banco de dados indisponível. Tente novamente em instantes."));
    }
}
