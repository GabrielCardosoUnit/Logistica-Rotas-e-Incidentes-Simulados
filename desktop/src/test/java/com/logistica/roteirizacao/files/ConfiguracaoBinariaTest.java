package com.logistica.roteirizacao.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistica.roteirizacao.client.dto.Criterio;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracaoBinariaTest {

    @TempDir
    Path pasta;

    private File arquivo() {
        return pasta.resolve("config.bin").toFile();
    }

    @Test
    void gravarELerDevolveAMesmaConfiguracao() throws IOException {
        Configuracao original = new Configuracao(Criterio.MENOR_TEMPO, "P05", "P01", "http://localhost:8081");

        ConfiguracaoBinaria.salvar(original, arquivo());
        Configuracao lida = ConfiguracaoBinaria.ler(arquivo());

        assertEquals(original, lida);
        assertTrue(lida.temUltimaRota());
    }

    @Test
    void configuracaoPadraoSemUltimaRotaTambemFazIdaEVolta() throws IOException {
        ConfiguracaoBinaria.salvar(Configuracao.padrao(), arquivo());

        Configuracao lida = ConfiguracaoBinaria.ler(arquivo());

        assertEquals(Configuracao.padrao(), lida);
        assertFalse(lida.temUltimaRota());
    }

    @Test
    void arquivoSegueOLayoutDocumentado() throws IOException {
        ConfiguracaoBinaria.salvar(new Configuracao(Criterio.MENOR_TEMPO, "P12", "P07", "http://localhost:8080"),
                arquivo());

        try (DataInputStream in = new DataInputStream(new FileInputStream(arquivo()))) {
            assertEquals(1, in.readInt(), "versão do formato");
            assertEquals(1, in.readInt(), "critério: 1 = menor tempo");
            assertEquals("P12", in.readUTF());
            assertEquals("P07", in.readUTF());
            assertEquals("http://localhost:8080", in.readUTF());
            assertEquals(-1, in.read(), "não deve haver bytes depois da URL");
        }
    }

    @Test
    void arquivoAusenteGeraErroTratado() {
        ConfiguracaoInvalidaException e = assertThrows(ConfiguracaoInvalidaException.class,
                () -> ConfiguracaoBinaria.ler(arquivo()));

        assertTrue(e.getMessage().contains("não encontrado"), e.getMessage());
    }

    @Test
    void arquivoTruncadoGeraErroTratado() throws IOException {
        ConfiguracaoBinaria.salvar(new Configuracao(Criterio.MENOR_DISTANCIA, "P05", "P01",
                "http://localhost:8080"), arquivo());
        byte[] completo = Files.readAllBytes(arquivo().toPath());
        Files.write(arquivo().toPath(), Arrays.copyOf(completo, completo.length - 5));

        ConfiguracaoInvalidaException e = assertThrows(ConfiguracaoInvalidaException.class,
                () -> ConfiguracaoBinaria.ler(arquivo()));

        assertTrue(e.getMessage().contains("truncado"), e.getMessage());
    }

    @Test
    void versaoNaoSuportadaGeraErroTratado() throws IOException {
        gravarBruto(2, 0, "P05", "P01", "http://localhost:8080");

        ConfiguracaoInvalidaException e = assertThrows(ConfiguracaoInvalidaException.class,
                () -> ConfiguracaoBinaria.ler(arquivo()));

        assertTrue(e.getMessage().contains("Versão 2"), e.getMessage());
    }

    @Test
    void criterioDesconhecidoGeraErroTratado() throws IOException {
        gravarBruto(1, 7, "P05", "P01", "http://localhost:8080");

        ConfiguracaoInvalidaException e = assertThrows(ConfiguracaoInvalidaException.class,
                () -> ConfiguracaoBinaria.ler(arquivo()));

        assertTrue(e.getMessage().contains("Critério 7"), e.getMessage());
    }

    @Test
    void urlInvalidaGeraErroTratado() throws IOException {
        gravarBruto(1, 0, "P05", "P01", "localhost:8080");

        ConfiguracaoInvalidaException e = assertThrows(ConfiguracaoInvalidaException.class,
                () -> ConfiguracaoBinaria.ler(arquivo()));

        assertTrue(e.getMessage().contains("valor inválido"), e.getMessage());
    }

    @Test
    void bytesSobrandoGeramErroTratado() throws IOException {
        ConfiguracaoBinaria.salvar(Configuracao.padrao(), arquivo());
        Files.write(arquivo().toPath(), new byte[] {42}, java.nio.file.StandardOpenOption.APPEND);

        ConfiguracaoInvalidaException e = assertThrows(ConfiguracaoInvalidaException.class,
                () -> ConfiguracaoBinaria.ler(arquivo()));

        assertTrue(e.getMessage().contains("além do esperado"), e.getMessage());
    }

    @Test
    void urlSemHttpEhRecusadaNaCriacao() {
        assertThrows(IllegalArgumentException.class,
                () -> new Configuracao(Criterio.MENOR_DISTANCIA, "", "", "ftp://servidor"));
    }

    /** Grava os campos sem passar pela validação, para simular um arquivo ruim. */
    private void gravarBruto(int versao, int criterio, String origem, String destino, String url) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(arquivo()))) {
            out.writeInt(versao);
            out.writeInt(criterio);
            out.writeUTF(origem);
            out.writeUTF(destino);
            out.writeUTF(url);
        }
    }
}
