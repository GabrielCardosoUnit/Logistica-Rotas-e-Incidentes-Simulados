package com.logistica.roteirizacao.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import com.logistica.roteirizacao.exception.CaminhoInexistenteException;

/** Dijkstra sobre o {@link Grafo}, com o peso definido por um {@link CriterioRota}. */
public class CalculadorRota {

    private record Candidato(String pontoId, double custo) {
    }

    /** Critério padrão: menor distância. */
    public Rota calcular(Grafo grafo, String origemId, String destinoId) {
        return calcular(grafo, origemId, destinoId, new MenorDistancia());
    }

    /**
     * @throws IllegalArgumentException se origem ou destino não estão no grafo
     * @throws CaminhoInexistenteException se o destino não é alcançável sem vias bloqueadas
     */
    public Rota calcular(Grafo grafo, String origemId, String destinoId, CriterioRota criterio) {
        Objects.requireNonNull(grafo, "grafo é obrigatório.");
        Objects.requireNonNull(criterio, "critério é obrigatório.");
        if (!grafo.contemPonto(origemId)) {
            throw new IllegalArgumentException("Ponto de origem " + origemId + " não existe no grafo.");
        }
        if (!grafo.contemPonto(destinoId)) {
            throw new IllegalArgumentException("Ponto de destino " + destinoId + " não existe no grafo.");
        }

        Map<String, Double> custo = new HashMap<>();
        Map<String, Grafo.Trecho> chegouPor = new HashMap<>();
        Set<String> fechados = new HashSet<>();
        // Empate de custo é desfeito pelo id do ponto, para o resultado não variar.
        PriorityQueue<Candidato> fila = new PriorityQueue<>(
                Comparator.comparingDouble(Candidato::custo).thenComparing(Candidato::pontoId));

        custo.put(origemId, 0.0);
        fila.add(new Candidato(origemId, 0.0));

        while (!fila.isEmpty()) {
            Candidato atual = fila.poll();
            if (!fechados.add(atual.pontoId())) {
                continue; // entrada antiga na fila, já processada com custo menor
            }
            if (atual.pontoId().equals(destinoId)) {
                break;
            }
            for (Grafo.Trecho trecho : grafo.trechosSaindoDe(atual.pontoId())) {
                if (grafo.estaBloqueada(trecho.via().getId()) || fechados.contains(trecho.para())) {
                    continue;
                }
                double novoCusto = atual.custo() + criterio.peso(trecho.via());
                if (novoCusto < custo.getOrDefault(trecho.para(), Double.POSITIVE_INFINITY)) {
                    custo.put(trecho.para(), novoCusto);
                    chegouPor.put(trecho.para(), trecho);
                    fila.add(new Candidato(trecho.para(), novoCusto));
                }
            }
        }

        if (!fechados.contains(destinoId)) {
            throw new CaminhoInexistenteException(origemId, destinoId, grafo.viasBloqueadas());
        }
        return montarRota(origemId, destinoId, criterio, chegouPor);
    }

    private Rota montarRota(String origemId, String destinoId, CriterioRota criterio,
                            Map<String, Grafo.Trecho> chegouPor) {
        Deque<String> pontos = new ArrayDeque<>();
        Deque<String> vias = new ArrayDeque<>();
        double distancia = 0;
        double tempo = 0;

        String atual = destinoId;
        pontos.addFirst(atual);
        while (!atual.equals(origemId)) {
            Grafo.Trecho trecho = chegouPor.get(atual);
            vias.addFirst(trecho.via().getId());
            distancia += trecho.via().getDistanciaKm();
            tempo += trecho.via().getTempoMin();
            atual = trecho.de();
            pontos.addFirst(atual);
        }
        return new Rota(origemId, destinoId, criterio.descricao(),
                new ArrayList<>(pontos), List.copyOf(vias), distancia, tempo);
    }
}
