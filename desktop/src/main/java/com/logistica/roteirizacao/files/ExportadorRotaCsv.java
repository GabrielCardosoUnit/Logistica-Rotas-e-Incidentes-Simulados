package com.logistica.roteirizacao.files;

import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.RotaResultado;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exporta uma rota calculada para CSV (UTF-8, separador vírgula, ponto como separador decimal)
 * e lê o arquivo de volta.
 *
 * <p>Uma linha por ponto do percurso, na ordem. Os dados da rota se repetem em todas as linhas
 * para que cada linha seja completa ao abrir numa planilha:
 * <pre>
 * rotaId,criterio,ordem,pontoId,pontoNome,viaChegadaId,distanciaTotalKm,tempoTotalMin,versaoRede,calculadaEm
 * 1,MENOR_DISTANCIA,1,P05,Rodoviária de Aracaju,,8.5,14.5,1,2026-11-10T10:15-03:00
 * 1,MENOR_DISTANCIA,2,P15,Distrito Industrial de Aracaju (DIA),V14,8.5,14.5,1,2026-11-10T10:15-03:00
 * </pre>
 * {@code viaChegadaId} é a via usada para chegar ao ponto (vazia no primeiro).
 */
public final class ExportadorRotaCsv {

    public static final String CABECALHO = "rotaId,criterio,ordem,pontoId,pontoNome,viaChegadaId,"
            + "distanciaTotalKm,tempoTotalMin,versaoRede,calculadaEm";

    private static final int COLUNAS = 10;

    private ExportadorRotaCsv() {
    }

    /**
     * @param nomesPorId nome de cada ponto, para o arquivo ficar legível (ponto sem nome sai vazio)
     * @throws IllegalArgumentException se a rota é "sem caminho": não há percurso para exportar
     */
    public static void exportar(RotaResultado rota, Map<String, String> nomesPorId, File arquivo) throws IOException {
        Objects.requireNonNull(rota, "rota é obrigatória.");
        Objects.requireNonNull(nomesPorId, "nomes dos pontos são obrigatórios.");
        Objects.requireNonNull(arquivo, "arquivo é obrigatório.");
        if (rota.semCaminho() || rota.pontos().isEmpty()) {
            throw new IllegalArgumentException("Não há percurso para exportar: a rota está sem caminho.");
        }

        List<String> linhas = new ArrayList<>();
        linhas.add(CABECALHO);
        List<String> pontos = rota.pontos();
        for (int i = 0; i < pontos.size(); i++) {
            String pontoId = pontos.get(i);
            linhas.add(Csv.linha(
                    String.valueOf(rota.id()),
                    rota.criterio().name(),
                    String.valueOf(i + 1),
                    pontoId,
                    nomesPorId.getOrDefault(pontoId, ""),
                    i == 0 ? "" : rota.vias().get(i - 1),
                    String.valueOf(rota.distanciaTotalKm()),
                    String.valueOf(rota.tempoTotalMin()),
                    String.valueOf(rota.versaoRede()),
                    rota.calculadaEm() == null ? "" : rota.calculadaEm().toString()));
        }
        Files.write(arquivo.toPath(), linhas, StandardCharsets.UTF_8);
    }

    /** Lê um CSV gerado por {@link #exportar} e reconstrói a rota. */
    public static RotaResultado ler(File arquivo) throws IOException {
        Objects.requireNonNull(arquivo, "arquivo é obrigatório.");
        if (!arquivo.isFile()) {
            throw new IOException("Arquivo " + arquivo.getName() + " não encontrado.");
        }
        List<String> linhas = Files.readAllLines(arquivo.toPath(), StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !CABECALHO.equals(linhas.get(0).replace("﻿", "").trim())) {
            throw new IOException("Cabeçalho inválido em " + arquivo.getName() + ". Esperado: " + CABECALHO + ".");
        }

        List<String> pontos = new ArrayList<>();
        List<String> vias = new ArrayList<>();
        List<String> primeira = null;
        for (int n = 1; n < linhas.size(); n++) {
            if (linhas.get(n).isBlank()) {
                continue;
            }
            List<String> c = Csv.separar(linhas.get(n));
            if (c.size() != COLUNAS) {
                throw new IOException("Linha " + (n + 1) + " de " + arquivo.getName() + " tem " + c.size()
                        + " colunas (esperadas: " + COLUNAS + ").");
            }
            if (primeira == null) {
                primeira = c;
            }
            pontos.add(c.get(3));
            if (!c.get(5).isEmpty()) {
                vias.add(c.get(5));
            }
        }
        if (primeira == null) {
            throw new IOException(arquivo.getName() + " não tem nenhum ponto da rota.");
        }

        try {
            String calculadaEm = primeira.get(9);
            return new RotaResultado(
                    Long.parseLong(primeira.get(0)),
                    pontos.get(0),
                    pontos.get(pontos.size() - 1),
                    Criterio.valueOf(primeira.get(1)),
                    "CALCULADA",
                    pontos,
                    vias,
                    Double.valueOf(primeira.get(6)),
                    Double.valueOf(primeira.get(7)),
                    Integer.parseInt(primeira.get(8)),
                    null,
                    calculadaEm.isEmpty() ? null : OffsetDateTime.parse(calculadaEm));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            // NumberFormatException e critério desconhecido são IllegalArgumentException
            throw new IOException("Valor inválido em " + arquivo.getName() + ": " + e.getMessage(), e);
        }
    }
}
