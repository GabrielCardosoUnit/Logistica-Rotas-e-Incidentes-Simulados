package com.logistica.roteirizacao.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RedeControllerTest extends ApiIntegracaoTeste {

    @Test
    void importaARedeEIncrementaAVersao() throws Exception {
        int antes = corpo(importarRede("validacao")).get("versaoRede").asInt();

        importarRede("validacao")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versaoRede").value(antes + 1))
                .andExpect(jsonPath("$.pontosImportados").value(5))
                .andExpect(jsonPath("$.viasImportadas").value(6))
                .andExpect(jsonPath("$.linhasRejeitadas", empty()));
    }

    @Test
    void listaAsLinhasRejeitadas() throws Exception {
        importarRede("com-erros")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontosImportados").value(2))
                .andExpect(jsonPath("$.viasImportadas").value(1))
                .andExpect(jsonPath("$.linhasRejeitadas", hasSize(7)))
                .andExpect(jsonPath("$.linhasRejeitadas[2].arquivo").value("vias.csv"))
                .andExpect(jsonPath("$.linhasRejeitadas[2].linha").value(3))
                .andExpect(jsonPath("$.linhasRejeitadas[2].motivo")
                        .value("distanciaKm deve ser maior que zero (valor: -1.2)."));
    }

    @Test
    void cabecalhoInvalidoGera400() throws Exception {
        importar("id,nome,latitude,longitude\nA,A,0,0\n".getBytes(StandardCharsets.UTF_8),
                "id;origem;destino\n".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO"))
                .andExpect(jsonPath("$.campo").value("vias"))
                .andExpect(jsonPath("$.mensagem").value(
                        "Cabeçalho inválido em vias.csv. Esperado: id,origem,destino,distanciaKm,tempoMin,habilitada."));
    }

    @Test
    void semNenhumaViaValidaGera400EMantemARedeAnterior() throws Exception {
        importarRede("validacao").andExpect(status().isCreated());

        importar("id,nome,latitude,longitude\nA,A,0,0\nB,B,0,0\n".getBytes(StandardCharsets.UTF_8),
                "id,origem,destino,distanciaKm,tempoMin,habilitada\nV1,A,B,-3,5,true\n".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("vias"));

        calcular("A", "E", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontos", contains("A", "B", "D", "E")));
    }

    @Test
    void arquivoDeViasAusenteGera400() throws Exception {
        mvc.perform(multipart("/redes/importacoes")
                        .file(new MockMultipartFile("pontos", "pontos.csv", "text/csv",
                                "id,nome,latitude,longitude\n".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("vias"));
    }

    /** Reproduz os exemplos do docs/openapi.yaml com a rede real de Aracaju. */
    @Test
    void redeDoProjetoConfereComOsExemplosDoContrato() throws Exception {
        importarRedeDoProjeto()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontosImportados").value(20))
                .andExpect(jsonPath("$.viasImportadas").value(30));

        calcular("P05", "P01", "MENOR_DISTANCIA")
                .andExpect(jsonPath("$.pontos", contains("P05", "P15", "P01")))
                .andExpect(jsonPath("$.vias", contains("V14", "V24")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(8.5))
                .andExpect(jsonPath("$.tempoTotalMin").value(14.5));

        bloquear("V24", "Alagamento simulado na Av. Tancredo Neves").andExpect(status().isCreated());

        calcular("P05", "P01", "MENOR_DISTANCIA")
                .andExpect(jsonPath("$.pontos", contains("P05", "P15", "P03", "P01")))
                .andExpect(jsonPath("$.vias", contains("V14", "V23", "V25")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(11.7))
                .andExpect(jsonPath("$.tempoTotalMin").value(23.2));
    }
}
