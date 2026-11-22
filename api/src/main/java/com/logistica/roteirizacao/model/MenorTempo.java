package com.logistica.roteirizacao.model;

/** Critério que usa o tempo de percurso da via (min) como peso. */
public class MenorTempo extends CriterioRotaBase {

    /** Cria o critério. */
    public MenorTempo() {
    }

    @Override
    protected double pesoDe(Via via) {
        return via.getTempoMin();
    }

    @Override
    public String descricao() {
        return "menor tempo (min)";
    }
}
