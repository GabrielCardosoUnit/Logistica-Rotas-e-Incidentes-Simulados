package com.logistica.roteirizacao.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

class RotaControllerTest extends ApiIntegracaoTeste {

    @BeforeEach
    void carregarRede() throws Exception {
        importarRede("validacao").andExpect(status().isCreated());
    }

    @Test
    void calculaGravaEConsultaAMesmaRota() throws Exception {
        JsonNode criada = corpo(calcular("A", "E", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CALCULADA"))
                .andExpect(jsonPath("$.pontos", contains("A", "B", "D", "E")))
                .andExpect(jsonPath("$.vias", contains("V1", "V3", "V6")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(11.0))
                .andExpect(jsonPath("$.tempoTotalMin").value(22.0))
                .andExpect(jsonPath("$.mensagem").value(nullValue())));
        long id = criada.get("id").asLong();

        mvc.perform(get("/rotas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.pontos", contains("A", "B", "D", "E")))
                .andExpect(jsonPath("$.versaoRede").value(criada.get("versaoRede").asInt()))
                .andExpect(jsonPath("$.calculadaEm").value(criada.get("calculadaEm").asText()));
    }

    @Test
    void menorTempoRespondeComLocationDaRotaCriada() throws Exception {
        JsonNode criada = corpo(calcular("A", "E", "MENOR_TEMPO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontos", contains("A", "C", "E")))
                .andExpect(jsonPath("$.tempoTotalMin").value(19.0)));

        mvc.perform(get("/rotas/{id}", criada.get("id").asLong()))
                .andExpect(status().isOk());
        calcular("A", "E", "MENOR_TEMPO")
                .andExpect(header().string("Location", startsWith("/rotas/")));
    }

    @Test
    void semCaminhoResponde201ComStatusSemCaminho() throws Exception {
        bloquear("V5", "Alagamento simulado").andExpect(status().isCreated());
        bloquear("V6", "Obra simulada").andExpect(status().isCreated());

        calcular("A", "E", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SEM_CAMINHO"))
                .andExpect(jsonPath("$.pontos", empty()))
                .andExpect(jsonPath("$.vias", empty()))
                .andExpect(jsonPath("$.distanciaTotalKm").value(nullValue()))
                .andExpect(jsonPath("$.mensagem")
                        .value("Não há caminho disponível de A até E. Vias bloqueadas: V5, V6."));
    }

    @Test
    void origemIgualAoDestino() throws Exception {
        calcular("B", "B", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontos", contains("B")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(0.0));
    }

    @Test
    void pontoDeOrigemInexistenteGera404() throws Exception {
        calcular("P99", "E", "MENOR_DISTANCIA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.campo").value("origemId"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Ponto P99 não existe na rede. Importe a rede ou escolha outro ponto."));
    }

    @Test
    void criterioInvalidoGera400NoFormatoPadrao() throws Exception {
        calcular("A", "E", "MAIS_BONITO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("VALIDACAO"))
                .andExpect(jsonPath("$.campo").value("criterio"))
                .andExpect(jsonPath("$.mensagem").value("Critério inválido. Use MENOR_DISTANCIA ou MENOR_TEMPO."));
    }

    @Test
    void campoAusenteGera400ComONomeDoCampo() throws Exception {
        postJson("/rotas", "{\"destinoId\":\"E\",\"criterio\":\"MENOR_TEMPO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("origemId"))
                .andExpect(jsonPath("$.mensagem").value("Informe o ponto de origem."));
    }

    @Test
    void jsonMalFormadoGera400() throws Exception {
        postJson("/rotas", "{origemId: A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO"))
                .andExpect(jsonPath("$.campo").value(nullValue()));
    }

    @Test
    void rotaInexistenteGera404() throws Exception {
        mvc.perform(get("/rotas/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.campo").value("id"))
                .andExpect(jsonPath("$.mensagem").value("Rota 987654 não encontrada."));
    }

    @Test
    void idQueNaoEhNumeroGera400() throws Exception {
        mvc.perform(get("/rotas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO"))
                .andExpect(jsonPath("$.campo").value("id"));
    }
}
