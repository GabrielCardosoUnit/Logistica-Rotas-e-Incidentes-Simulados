package com.logistica.roteirizacao.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.logistica.roteirizacao.exception.ValidacaoException;
import com.logistica.roteirizacao.model.Ponto;
import com.logistica.roteirizacao.model.Via;

/**
 * Lê pontos.csv e vias.csv (UTF-8, separador vírgula, com cabeçalho).
 *
 * <p>Cabeçalho errado invalida o arquivo inteiro (400). Linhas com problema
 * são rejeitadas uma a uma e o restante é importado. As regras de valor
 * (peso > 0, coordenadas, origem ≠ destino) ficam nos construtores de
 * {@link Ponto} e {@link Via}, para não duplicar validação.
 */
public class ImportadorRedeCsv {

    public static final String ARQUIVO_PONTOS = "pontos.csv";
    public static final String ARQUIVO_VIAS = "vias.csv";
    public static final String CABECALHO_PONTOS = "id,nome,latitude,longitude";
    public static final String CABECALHO_VIAS = "id,origem,destino,distanciaKm,tempoMin,habilitada";

    public ResultadoImportacao ler(InputStream pontosCsv, InputStream viasCsv) {
        List<LinhaRejeitada> rejeitadas = new ArrayList<>();
        Map<String, Ponto> pontos = lerPontos(pontosCsv, rejeitadas);
        List<Via> vias = lerVias(viasCsv, pontos, rejeitadas);
        return new ResultadoImportacao(new ArrayList<>(pontos.values()), vias, rejeitadas);
    }

    private Map<String, Ponto> lerPontos(InputStream csv, List<LinhaRejeitada> rejeitadas) {
        Map<String, Ponto> pontos = new LinkedHashMap<>();
        Map<String, Integer> linhaDoId = new HashMap<>();
        List<String> linhas = lerLinhas(csv, "pontos", ARQUIVO_PONTOS, CABECALHO_PONTOS);

        for (int i = 1; i < linhas.size(); i++) {
            int numero = i + 1;
            String linha = linhas.get(i);
            if (linha.isBlank()) {
                continue;
            }
            try {
                String[] c = campos(linha, 4);
                String id = obrigatorio(c[0], "id");
                Integer anterior = linhaDoId.get(id);
                if (anterior != null) {
                    throw new IllegalArgumentException("Id " + id + " duplicado (já usado na linha " + anterior + ").");
                }
                Ponto ponto = new Ponto(id, obrigatorio(c[1], "nome"),
                        numero(c[2], "latitude"), numero(c[3], "longitude"));
                pontos.put(ponto.getId(), ponto);
                linhaDoId.put(ponto.getId(), numero);
            } catch (IllegalArgumentException e) {
                rejeitadas.add(new LinhaRejeitada(ARQUIVO_PONTOS, numero, e.getMessage()));
            }
        }
        return pontos;
    }

    private List<Via> lerVias(InputStream csv, Map<String, Ponto> pontos, List<LinhaRejeitada> rejeitadas) {
        List<Via> vias = new ArrayList<>();
        Map<String, Integer> linhaDoId = new HashMap<>();
        List<String> linhas = lerLinhas(csv, "vias", ARQUIVO_VIAS, CABECALHO_VIAS);

        for (int i = 1; i < linhas.size(); i++) {
            int numero = i + 1;
            String linha = linhas.get(i);
            if (linha.isBlank()) {
                continue;
            }
            try {
                String[] c = campos(linha, 6);
                String id = obrigatorio(c[0], "id");
                Integer anterior = linhaDoId.get(id);
                if (anterior != null) {
                    throw new IllegalArgumentException("Id " + id + " duplicado (já usado na linha " + anterior + ").");
                }
                Ponto origem = pontoExistente(pontos, obrigatorio(c[1], "origem"), "origem");
                Ponto destino = pontoExistente(pontos, obrigatorio(c[2], "destino"), "destino");
                Via via = new Via(id, origem, destino,
                        numero(c[3], "distanciaKm"), numero(c[4], "tempoMin"), booleano(c[5], "habilitada"));
                vias.add(via);
                linhaDoId.put(via.getId(), numero);
            } catch (IllegalArgumentException e) {
                rejeitadas.add(new LinhaRejeitada(ARQUIVO_VIAS, numero, e.getMessage()));
            }
        }
        return vias;
    }

    private List<String> lerLinhas(InputStream csv, String campo, String arquivo, String cabecalhoEsperado) {
        if (csv == null) {
            throw new ValidacaoException(campo, "Envie o arquivo " + arquivo + ".");
        }
        List<String> linhas = new ArrayList<>();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linhas.add(linha);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + arquivo, e);
        }
        if (linhas.isEmpty()) {
            throw new ValidacaoException(campo, "O arquivo " + arquivo + " está vazio.");
        }
        // Planilhas costumam gravar BOM no início do UTF-8; ele não faz parte do cabeçalho.
        String cabecalho = linhas.get(0).replace("﻿", "").replace(" ", "").strip();
        if (!cabecalho.equals(cabecalhoEsperado)) {
            throw new ValidacaoException(campo,
                    "Cabeçalho inválido em " + arquivo + ". Esperado: " + cabecalhoEsperado + ".");
        }
        return linhas;
    }

    /**
     * Separa a linha por vírgulas, respeitando campos entre aspas
     * ("Parque, Centro" vira um único campo e "" vira uma aspa).
     */
    static String[] campos(String linha, int esperado) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char ch = linha.charAt(i);
            if (ch == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (ch == ',' && !entreAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(ch);
            }
        }
        campos.add(atual.toString());
        if (campos.size() != esperado) {
            throw new IllegalArgumentException(
                    "Esperados " + esperado + " campos, encontrados " + campos.size() + ".");
        }
        return campos.toArray(new String[0]);
    }

    private static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Campo " + campo + " vazio.");
        }
        return valor.strip();
    }

    private static double numero(String valor, String campo) {
        String texto = obrigatorio(valor, campo);
        try {
            double numero = Double.parseDouble(texto);
            if (!Double.isFinite(numero)) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " não é um número válido (valor: " + texto
                    + "). Use ponto como separador decimal.");
        }
    }

    private static boolean booleano(String valor, String campo) {
        String texto = obrigatorio(valor, campo).toLowerCase(Locale.ROOT);
        return switch (texto) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(campo + " deve ser true ou false (valor: " + valor.strip() + ").");
        };
    }

    private static Ponto pontoExistente(Map<String, Ponto> pontos, String id, String papel) {
        Ponto ponto = pontos.get(id);
        if (ponto == null) {
            throw new IllegalArgumentException("Ponto de " + papel + " " + id + " não existe em " + ARQUIVO_PONTOS + ".");
        }
        return ponto;
    }
}
