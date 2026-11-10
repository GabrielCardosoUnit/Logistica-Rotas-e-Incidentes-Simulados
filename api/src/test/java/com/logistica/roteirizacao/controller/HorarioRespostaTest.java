package com.logistica.roteirizacao.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * O relógio do sistema pode ter precisão de nanossegundos (no Windows, 100 ns), mas o banco
 * grava só microssegundos. Sem truncar, o POST devolvia um horário e o GET da mesma rota outro.
 * Aqui o relógio é fixo e tem nanossegundos, para o teste falhar sempre que o bug voltar.
 */
@Import(HorarioRespostaTest.RelogioComNanossegundos.class)
class HorarioRespostaTest extends ApiIntegracaoTeste {

    /** 13:15:00.123456789 UTC = 10:15:00.123456789 em Brasília. */
    private static final Instant INSTANTE = Instant.parse("2026-11-10T13:15:00.123456789Z");
    private static final String ESPERADO = "2026-11-10T10:15:00.123456-03:00";

    @TestConfiguration
    static class RelogioComNanossegundos {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(INSTANTE, ZoneId.of("America/Sao_Paulo"));
        }
    }

    @BeforeEach
    void carregarRede() throws Exception {
        importarRede("validacao").andExpect(status().isCreated());
    }

    @Test
    void postEGetDaMesmaRotaDevolvemOMesmoHorarioEmMicrossegundos() throws Exception {
        JsonNode criada = corpo(calcular("A", "E", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calculadaEm").value(ESPERADO)));

        mvc.perform(get("/rotas/{id}", criada.get("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculadaEm").value(ESPERADO));
    }

    @Test
    void rotaSemCaminhoTambemSaiEmMicrossegundos() throws Exception {
        bloquear("V5", "Alagamento simulado").andExpect(status().isCreated());
        bloquear("V6", "Alagamento simulado").andExpect(status().isCreated());

        JsonNode criada = corpo(calcular("A", "E", "MENOR_DISTANCIA")
                .andExpect(jsonPath("$.status").value("SEM_CAMINHO"))
                .andExpect(jsonPath("$.calculadaEm").value(ESPERADO)));

        mvc.perform(get("/rotas/{id}", criada.get("id").asLong()))
                .andExpect(jsonPath("$.calculadaEm").value(ESPERADO));
    }

    @Test
    void incidenteRegistradoSaiEmMicrossegundos() throws Exception {
        bloquear("V3", "Alagamento simulado")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradoEm").value(ESPERADO));
    }
}
