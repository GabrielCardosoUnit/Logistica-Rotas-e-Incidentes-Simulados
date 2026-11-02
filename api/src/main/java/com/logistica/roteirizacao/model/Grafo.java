package com.logistica.roteirizacao.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * TAD da rede: pontos indexados por id e lista de adjacências por ponto.
 *
 * <p>Map dá acesso O(1) ao ponto e aos trechos que saem dele; LinkedHashMap
 * mantém a ordem de inserção para o resultado ser estável entre execuções.
 * As vias bloqueadas ficam num TreeSet para sair ordenadas nas mensagens.
 */
public class Grafo {

    /** Uma direção percorrível de uma via (a via é de mão dupla). */
    public record Trecho(Via via, String de, String para) {
    }

    private final Map<String, Ponto> pontos = new LinkedHashMap<>();
    private final Map<String, List<Trecho>> adjacencias = new LinkedHashMap<>();
    private final Set<String> viasBloqueadas = new TreeSet<>();
    private final Set<String> viasConhecidas = new TreeSet<>();

    public void adicionarPonto(Ponto ponto) {
        Objects.requireNonNull(ponto, "ponto é obrigatório.");
        if (pontos.putIfAbsent(ponto.getId(), ponto) != null) {
            throw new IllegalArgumentException("Ponto " + ponto.getId() + " já existe no grafo.");
        }
        adjacencias.put(ponto.getId(), new ArrayList<>());
    }

    /** Adiciona os dois sentidos da via. Via desabilitada já entra bloqueada. */
    public void adicionarVia(Via via) {
        Objects.requireNonNull(via, "via é obrigatória.");
        String origem = via.getOrigem().getId();
        String destino = via.getDestino().getId();
        exigirPonto(origem);
        exigirPonto(destino);
        if (!viasConhecidas.add(via.getId())) {
            throw new IllegalArgumentException("Via " + via.getId() + " já existe no grafo.");
        }
        adjacencias.get(origem).add(new Trecho(via, origem, destino));
        adjacencias.get(destino).add(new Trecho(via, destino, origem));
        if (!via.isHabilitada()) {
            viasBloqueadas.add(via.getId());
        }
    }

    public void bloquearVia(String viaId) {
        if (!viasConhecidas.contains(viaId)) {
            throw new IllegalArgumentException("Via " + viaId + " não existe no grafo.");
        }
        viasBloqueadas.add(viaId);
    }

    public boolean contemPonto(String pontoId) {
        return pontos.containsKey(pontoId);
    }

    public boolean estaBloqueada(String viaId) {
        return viasBloqueadas.contains(viaId);
    }

    public List<Trecho> trechosSaindoDe(String pontoId) {
        exigirPonto(pontoId);
        return Collections.unmodifiableList(adjacencias.get(pontoId));
    }

    /** Ids das vias bloqueadas, em ordem crescente. */
    public List<String> viasBloqueadas() {
        return List.copyOf(viasBloqueadas);
    }

    public int quantidadePontos() {
        return pontos.size();
    }

    public int quantidadeVias() {
        return viasConhecidas.size();
    }

    private void exigirPonto(String pontoId) {
        if (!pontos.containsKey(pontoId)) {
            throw new IllegalArgumentException("Ponto " + pontoId + " não existe no grafo.");
        }
    }

    @Override
    public String toString() {
        return "Grafo[" + pontos.size() + " pontos, " + viasConhecidas.size() + " vias, bloqueadas=" + viasBloqueadas + "]";
    }
}
