package com.logistica.roteirizacao.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

class IncidenteControllerTest extends ApiIntegracaoTeste {

    private int versaoInicial;

    @BeforeEach
    void carregarRede() throws Exception {
        versaoInicial = corpo(importarRede("validacao").andExpect(status().isCreated())).get("versaoRede").asInt();
    }

    @Test
    void bloquearUmaViaDaRotaMudaARotaRecalculada() throws Exception {
        calcular("A", "E", "MENOR_DISTANCIA").andExpect(jsonPath("$.pontos", contains("A", "B", "D", "E")));

        JsonNode incidente = corpo(bloquear("V3", "Alagamento simulado")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.viaId").value("V3"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.versaoRede").value(versaoInicial + 1)));

        calcular("A", "E", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontos", contains("A", "C", "D", "E")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(12.0))
                .andExpect(jsonPath("$.versaoRede").value(incidente.get("versaoRede").asInt()));
    }

    @Test
    void bloquearAMesmaViaDuasVezesGera409() throws Exception {
        long id = corpo(bloquear("V3", "Alagamento simulado")).get("id").asLong();

        bloquear("V3", "Outro motivo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("CONFLITO"))
                .andExpect(jsonPath("$.campo").value("viaId"))
                .andExpect(jsonPath("$.mensagem").value("A via V3 já está bloqueada pelo incidente " + id + "."));
    }

    @Test
    void viaInexistenteGera404() throws Exception {
        bloquear("V99", "Alagamento simulado")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.campo").value("viaId"))
                .andExpect(jsonPath("$.mensagem").value("Via V99 não existe na rede."));
    }

    @Test
    void descricaoMuitoCurtaGera400() throws Exception {
        bloquear("V3", "x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("descricao"))
                .andExpect(jsonPath("$.mensagem").value("A descrição deve ter entre 3 e 200 caracteres."));
    }

    @Test
    void viaAusenteGera400() throws Exception {
        postJson("/incidentes", "{\"descricao\":\"Alagamento\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("viaId"))
                .andExpect(jsonPath("$.mensagem").value("Informe a via a ser bloqueada."));
    }

    @Test
    void reimportarARedeLimpaOsIncidentes() throws Exception {
        bloquear("V3", "Alagamento simulado").andExpect(status().isCreated());

        importarRede("validacao").andExpect(status().isCreated());

        calcular("A", "E", "MENOR_DISTANCIA").andExpect(jsonPath("$.pontos", contains("A", "B", "D", "E")));
    }
}
