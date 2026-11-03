package com.logistica.roteirizacao.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê o pontos.csv escolhido pelo usuário só para preencher os combos e desenhar o mapa.
 *
 * <p>Decisão: a API não tem GET /pontos (ver README), então a GUI lê o mesmo arquivo que
 * enviou. Isso não é acesso ao banco. A validação "de verdade" continua na API; aqui linhas
 * malformadas são simplesmente ignoradas.
 */
public final class LeitorPontosCsv {

    private static final String CABECALHO = "id,nome,latitude,longitude";

    private LeitorPontosCsv() {
        // classe utilitária (método estático)
    }

    public static List<PontoRede> ler(File arquivo) throws IOException {
        if (arquivo == null || !arquivo.isFile()) {
            throw new IOException("Arquivo pontos.csv não encontrado. Escolha o arquivo novamente.");
        }
        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Não foi possível ler pontos.csv. Confirme que ele é um CSV UTF-8.", e);
        }
        if (linhas.isEmpty() || !CABECALHO.equals(linhas.get(0).replace("\uFEFF", "").trim())) {
            throw new IOException("Cabeçalho inválido em pontos.csv. Esperado: " + CABECALHO + ".");
        }

        Map<String, PontoRede> porId = new LinkedHashMap<>(); // mantém a ordem do arquivo
        for (String linha : linhas.subList(1, linhas.size())) {
            if (linha.isBlank()) {
                continue;
            }
            String[] c = linha.split(",", -1);
            if (c.length != 4) {
                continue;
            }
            try {
                PontoRede p = new PontoRede(c[0], c[1],
                        Double.parseDouble(c[2].trim()), Double.parseDouble(c[3].trim()));
                porId.putIfAbsent(p.getId(), p); // id duplicado: vale o primeiro
            } catch (IllegalArgumentException e) {
                // linha inválida: a API a rejeita e informa; aqui só ignoramos
            }
        }
        return new ArrayList<>(porId.values());
    }
}
