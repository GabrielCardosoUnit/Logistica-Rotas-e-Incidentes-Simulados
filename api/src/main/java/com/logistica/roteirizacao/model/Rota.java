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
     * Somas de double acumulam erro (5.5 + 2.5 + 3.7 = 11.700000000000001).
     * Os dados têm uma casa decimal; duas casas no resultado bastam.
     */
    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public String getOrigemId() {
        return origemId;
    }

    public String getDestinoId() {
        return destinoId;
    }

    public String getCriterio() {
        return criterio;
    }

    public List<String> getPontos() {
        return pontos;
    }

    public List<String> getVias() {
        return vias;
    }

    public double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }

    public double getTempoTotalMin() {
        return tempoTotalMin;
    }

    @Override
    public String toString() {
        return String.join(" -> ", pontos) + " por " + criterio
                + " (" + distanciaTotalKm + " km, " + tempoTotalMin + " min)";
    }
}
