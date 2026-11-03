package com.logistica.roteirizacao.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê o vias.csv escolhido pelo usuário só para desenhar as vias no mapa.
 * A validação de verdade (pesos, pontos existentes) continua na API; linhas malformadas são ignoradas.
 */
public final class LeitorViasCsv {

    private static final String CABECALHO = "id,origem,destino,distanciaKm,tempoMin,habilitada";

    private LeitorViasCsv() {
    }

    public static List<ViaRede> ler(File arquivo) throws IOException {
        if (arquivo == null || !arquivo.isFile()) {
            throw new IOException("Arquivo vias.csv não encontrado. Escolha o arquivo novamente.");
        }
        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Não foi possível ler vias.csv. Confirme que ele é um CSV UTF-8.", e);
        }
        if (linhas.isEmpty() || !CABECALHO.equals(linhas.get(0).replace("\uFEFF", "").trim())) {
            throw new IOException("Cabeçalho inválido em vias.csv. Esperado: " + CABECALHO + ".");
        }

        List<ViaRede> vias = new ArrayList<>();
        for (String linha : linhas.subList(1, linhas.size())) {
            if (linha.isBlank()) {
                continue;
            }
            String[] c = linha.split(",", -1);
            if (c.length != 6) {
                continue;
            }
            try {
                vias.add(new ViaRede(c[0], c[1], c[2], Boolean.parseBoolean(c[5].trim())));
            } catch (IllegalArgumentException e) {
                // linha inválida: a API a rejeita e informa; aqui só ignoramos
            }
        }
        return vias;
    }
}
