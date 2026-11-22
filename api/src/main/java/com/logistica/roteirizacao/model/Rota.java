package com.logistica.roteirizacao.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Resultado do Dijkstra: sequência de pontos e vias e os dois totais. */
public class Rota {

    private final String origemId;
    private final String destinoId;
    private final String criterio;
    private final List<String> pontos;
    private final List<String> vias;
    private final double distanciaTotalKm;
    private final double tempoTotalMin;

    /**
     * Cria a rota. Distância e tempo são arredondados com {@link #arredondar(double)}.
     *
     * @param origemId         ponto de partida
     * @param destinoId        ponto de chegada
     * @param criterio         descrição do critério usado
     * @param pontos           pontos percorridos, em ordem
     * @param vias             vias percorridas, em ordem
     * @param distanciaTotalKm soma das distâncias
     * @param tempoTotalMin    soma dos tempos
     */
    public Rota(String origemId, String destinoId, String criterio, List<String> pontos, List<String> vias,
                double distanciaTotalKm, double tempoTotalMin) {
        this.origemId = Objects.requireNonNull(origemId);
        this.destinoId = Objects.requireNonNull(destinoId);
        this.criterio = Objects.requireNonNull(criterio);
        this.pontos = List.copyOf(pontos);
        this.vias = List.copyOf(vias);
        this.distanciaTotalKm = arredondar(distanciaTotalKm);
        this.tempoTotalMin = arredondar(tempoTotalMin);
    }

    /**
     * Arredonda para duas casas. Somas de double acumulam erro
     * (5.5 + 2.5 + 3.7 = 11.700000000000001); os dados têm uma casa decimal,
     * então duas casas no resultado bastam.
     *
     * @param valor valor a arredondar
     * @return o valor com duas casas decimais
     */
    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Ponto de partida.
     *
     * @return id da origem
     */
    public String getOrigemId() {
        return origemId;
    }

    /**
     * Ponto de chegada.
     *
     * @return id do destino
     */
    public String getDestinoId() {
        return destinoId;
    }

    /**
     * Critério usado no cálculo.
     *
     * @return descrição do critério
     */
    public String getCriterio() {
        return criterio;
    }

    /**
     * Pontos percorridos.
     *
     * @return ids dos pontos, da origem ao destino
     */
    public List<String> getPontos() {
        return pontos;
    }

    /**
     * Vias percorridas.
     *
     * @return ids das vias, na ordem do percurso
     */
    public List<String> getVias() {
        return vias;
    }

    /**
     * Distância total.
     *
     * @return distância em km, com duas casas
     */
    public double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }

    /**
     * Tempo total.
     *
     * @return tempo em minutos, com duas casas
     */
    public double getTempoTotalMin() {
        return tempoTotalMin;
    }

    @Override
    public String toString() {
        return String.join(" -> ", pontos) + " por " + criterio
                + " (" + distanciaTotalKm + " km, " + tempoTotalMin + " min)";
    }
}
