package com.logistica.roteirizacao.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.logistica.roteirizacao.exception.ValidacaoException;

class ImportadorRedeCsvTest {

    private final ImportadorRedeCsv importador = new ImportadorRedeCsv();

    @Test
    void leOsDadosTratadosDoProjetoSemRejeicoes() throws IOException {
        // O Maven roda os testes a partir da pasta api/.
        Path dados = Path.of("..", "dados", "tratados");
        try (InputStream pontos = Files.newInputStream(dados.resolve("pontos.csv"));
             InputStream vias = Files.newInputStream(dados.resolve("vias.csv"))) {

            ResultadoImportacao r = importador.ler(pontos, vias);

            assertThat(r.pontos()).hasSize(20);
            assertThat(r.vias()).hasSize(30);
            assertThat(r.rejeitadas()).isEmpty();
        }
    }

    @Test
    void rejeitaLinhasInvalidasEImportaAsDemais() throws IOException {
        try (InputStream pontos = recurso("redes/com-erros/pontos.csv");
             InputStream vias = recurso("redes/com-erros/vias.csv")) {

            ResultadoImportacao r = importador.ler(pontos, vias);

            assertThat(r.pontos()).extracting("id").containsExactly("A", "B");
            assertThat(r.vias()).extracting("id").containsExactly("V1");
            assertThat(r.rejeitadas()).extracting(LinhaRejeitada::arquivo, LinhaRejeitada::linha)
                    .containsExactly(
                            tuple("pontos.csv", 4),
                            tuple("pontos.csv", 5),
                            tuple("vias.csv", 3),
                            tuple("vias.csv", 4),
                            tuple("vias.csv", 5),
                            tuple("vias.csv", 6),
                            tuple("vias.csv", 7));
            assertThat(r.rejeitadas()).extracting(LinhaRejeitada::motivo)
                    .contains("distanciaKm deve ser maior que zero (valor: -1.2).",
                            "Ponto de destino P99 não existe em pontos.csv.");
        }
    }

    @Test
    void cabecalhoErradoInvalidaOArquivo() {
        InputStream pontos = texto("id,nome,latitude,longitude\nA,A,0,0\n");
        InputStream vias = texto("id;origem;destino\n");

        assertThatThrownBy(() -> importador.ler(pontos, vias))
                .isInstanceOf(ValidacaoException.class)
                .hasMessageContaining("Cabeçalho inválido em vias.csv")
                .extracting("campo").isEqualTo("vias");
    }

    @Test
    void aceitaNomeEntreAspasComVirgulaEBomNoCabecalho() {
        InputStream pontos = texto("﻿id,nome,latitude,longitude\nA,\"Parque, Centro\",0,0\nB,B,0,0\n");
        InputStream vias = texto("id,origem,destino,distanciaKm,tempoMin,habilitada\nV1,A,B,1.5,3,TRUE\n");

        ResultadoImportacao r = importador.ler(pontos, vias);

        assertThat(r.pontos().get(0).getNome()).isEqualTo("Parque, Centro");
        assertThat(r.vias()).hasSize(1);
        assertThat(r.rejeitadas()).isEmpty();
    }

    private InputStream recurso(String caminho) {
        return getClass().getClassLoader().getResourceAsStream(caminho);
    }

    private static InputStream texto(String conteudo) {
        return new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
