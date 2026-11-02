package com.logistica.roteirizacao.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base dos testes REST: sobe a API inteira com H2 (perfil test) e oferece
 * atalhos para importar a rede, calcular rotas e registrar incidentes.
 * Cada teste importa a rede de novo, o que zera vias e incidentes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiIntegracaoTeste {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Importa pontos.csv e vias.csv de uma pasta em src/test/resources/redes. */
    protected ResultActions importarRede(String pasta) throws Exception {
        return importar(lerRecurso("redes/" + pasta + "/pontos.csv"), lerRecurso("redes/" + pasta + "/vias.csv"));
    }

    /** Importa a rede real do projeto (dados/tratados). */
    protected ResultActions importarRedeDoProjeto() throws Exception {
        Path dados = Path.of("..", "dados", "tratados");
        return importar(Files.readAllBytes(dados.resolve("pontos.csv")), Files.readAllBytes(dados.resolve("vias.csv")));
    }

    protected ResultActions importar(byte[] pontos, byte[] vias) throws Exception {
        return mvc.perform(multipart("/redes/importacoes")
                .file(new MockMultipartFile("pontos", "pontos.csv", "text/csv", pontos))
                .file(new MockMultipartFile("vias", "vias.csv", "text/csv", vias)));
    }

    protected ResultActions calcular(String origemId, String destinoId, String criterio) throws Exception {
        String corpo = json.createObjectNode()
                .put("origemId", origemId)
                .put("destinoId", destinoId)
                .put("criterio", criterio)
                .toString();
        return postJson("/rotas", corpo);
    }

    protected ResultActions bloquear(String viaId, String descricao) throws Exception {
        String corpo = json.createObjectNode().put("viaId", viaId).put("descricao", descricao).toString();
        return postJson("/incidentes", corpo);
    }

    protected ResultActions postJson(String url, String corpo) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    protected JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private byte[] lerRecurso(String caminho) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (in == null) {
                throw new IOException("Recurso de teste não encontrado: " + caminho);
            }
            return in.readAllBytes();
        }
    }
}
