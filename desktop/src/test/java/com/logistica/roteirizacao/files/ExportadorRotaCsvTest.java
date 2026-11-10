package com.logistica.roteirizacao.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.RotaResultado;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportadorRotaCsvTest {

    /** Rodoviária → UNIT por distância, como no contrato da API. */
    private static final RotaResultado RODOVIARIA_UNIT = new RotaResultado(
            1, "P05", "P01", Criterio.MENOR_DISTANCIA, "CALCULADA",
            List.of("P05", "P15", "P01"), List.of("V14", "V24"),
            8.5, 14.5, 1, null, OffsetDateTime.parse("2026-11-10T10:15:00-03:00"));

    private static final Map<String, String> NOMES = Map.of(
            "P05", "Rodoviária de Aracaju",
            "P15", "Distrito Industrial de Aracaju (DIA)",
            "P01", "Universidade Tiradentes (UNIT) - Farolândia");

    @TempDir
    Path pasta;

    private File arquivo() {
        return pasta.resolve("rota.csv").toFile();
    }

    @Test
    void exportarELerMantemOsDadosDaRota() throws IOException {
        ExportadorRotaCsv.exportar(RODOVIARIA_UNIT, NOMES, arquivo());

        RotaResultado lida = ExportadorRotaCsv.ler(arquivo());

        assertEquals(RODOVIARIA_UNIT, lida);
    }

    @Test
    void arquivoTemUmaLinhaPorPontoComAViaDeChegada() throws IOException {
        ExportadorRotaCsv.exportar(RODOVIARIA_UNIT, NOMES, arquivo());

        List<String> linhas = Files.readAllLines(arquivo().toPath(), StandardCharsets.UTF_8);

        assertEquals(4, linhas.size(), "cabeçalho + 3 pontos");
        assertEquals(ExportadorRotaCsv.CABECALHO, linhas.get(0));
        assertEquals("1,MENOR_DISTANCIA,1,P05,Rodoviária de Aracaju,,8.5,14.5,1,2026-11-10T10:15-03:00",
                linhas.get(1));
        assertTrue(linhas.get(2).contains(",P15,Distrito Industrial de Aracaju (DIA),V14,"), linhas.get(2));
        assertTrue(linhas.get(3).contains(",P01,Universidade Tiradentes (UNIT) - Farolândia,V24,"), linhas.get(3));
    }

    @Test
    void nomeComVirgulaEAspasSobreviveAIdaEVolta() throws IOException {
        Map<String, String> nomes = Map.of("P05", "Rodoviária, \"Nova\"", "P15", "DIA", "P01", "UNIT");
        ExportadorRotaCsv.exportar(RODOVIARIA_UNIT, nomes, arquivo());

        String linha = Files.readAllLines(arquivo().toPath(), StandardCharsets.UTF_8).get(1);

        assertTrue(linha.contains(",\"Rodoviária, \"\"Nova\"\"\","), linha);
        assertEquals(RODOVIARIA_UNIT, ExportadorRotaCsv.ler(arquivo()));
    }

    @Test
    void rotaSemCaminhoNaoPodeSerExportada() {
        RotaResultado semCaminho = new RotaResultado(3, "P05", "P07", Criterio.MENOR_DISTANCIA, "SEM_CAMINHO",
                List.of(), List.of(), null, null, 4, "Não há caminho.", OffsetDateTime.now());

        assertThrows(IllegalArgumentException.class,
                () -> ExportadorRotaCsv.exportar(semCaminho, NOMES, arquivo()));
    }

    @Test
    void cabecalhoErradoGeraErroAoLer() throws IOException {
        Files.writeString(arquivo().toPath(), "id,origem,destino\n1,P05,P01\n", StandardCharsets.UTF_8);

        IOException e = assertThrows(IOException.class, () -> ExportadorRotaCsv.ler(arquivo()));

        assertTrue(e.getMessage().contains("Cabeçalho inválido"), e.getMessage());
    }

    @Test
    void valorNumericoInvalidoGeraErroAoLer() throws IOException {
        Files.writeString(arquivo().toPath(), ExportadorRotaCsv.CABECALHO + "\n"
                + "1,MENOR_DISTANCIA,1,P05,Rodoviária,,oito,14.5,1,2026-11-10T10:15-03:00\n",
                StandardCharsets.UTF_8);

        IOException e = assertThrows(IOException.class, () -> ExportadorRotaCsv.ler(arquivo()));

        assertTrue(e.getMessage().contains("Valor inválido"), e.getMessage());
    }

    @Test
    void arquivoInexistenteGeraErroAoLer() {
        IOException e = assertThrows(IOException.class, () -> ExportadorRotaCsv.ler(arquivo()));

        assertTrue(e.getMessage().contains("não encontrado"), e.getMessage());
    }
}
