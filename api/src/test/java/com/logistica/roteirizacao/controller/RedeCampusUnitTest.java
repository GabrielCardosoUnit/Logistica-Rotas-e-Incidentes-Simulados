package com.logistica.roteirizacao.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Segunda rede do projeto: 13 pontos dentro do campus da UNIT (dados/tratados/unit), com
 * caminhos a pé. Confere a importação e os exemplos usados na documentação e na apresentação.
 */
class RedeCampusUnitTest extends ApiIntegracaoTeste {

    private static final Path REDE_UNIT = Path.of("..", "dados", "tratados", "unit");

    @BeforeEach
    void importarCampus() throws Exception {
        importar(Files.readAllBytes(REDE_UNIT.resolve("pontos.csv")), Files.readAllBytes(REDE_UNIT.resolve("vias.csv")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontosImportados").value(13))
                .andExpect(jsonPath("$.viasImportadas").value(21))
                .andExpect(jsonPath("$.linhasRejeitadas", empty()));
    }

    @Test
    void menorDistanciaPassaPeloTrechoMovimentado() throws Exception {
        calcular("U01", "U04", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pontos", contains("U01", "U03", "U04")))
                .andExpect(jsonPath("$.vias", contains("C03", "C07")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(0.33))
                .andExpect(jsonPath("$.tempoTotalMin").value(5.9));
    }

    @Test
    void menorTempoDesviaDoTrechoMovimentadoPeloBlocoD() throws Exception {
        calcular("U01", "U04", "MENOR_TEMPO")
                .andExpect(jsonPath("$.pontos", contains("U01", "U02", "U03", "U04")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(0.43))
                .andExpect(jsonPath("$.tempoTotalMin").value(5.2));
    }

    @Test
    void bloquearOTrechoPortariaBlocoAMudaARota() throws Exception {
        bloquear("C03", "Obra simulada no acesso ao Bloco A").andExpect(status().isCreated());

        calcular("U01", "U04", "MENOR_DISTANCIA")
                .andExpect(jsonPath("$.pontos", contains("U01", "U02", "U03", "U04")))
                .andExpect(jsonPath("$.distanciaTotalKm").value(0.43));
    }

    @Test
    void comunicacaoSocialIsoladaFicaSemCaminho() throws Exception {
        bloquear("C18", "Interdição simulada").andExpect(status().isCreated());
        bloquear("C19", "Interdição simulada").andExpect(status().isCreated());

        calcular("U01", "U11", "MENOR_DISTANCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SEM_CAMINHO"))
                .andExpect(jsonPath("$.pontos", empty()));
    }
}
