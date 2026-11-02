package com.logistica.roteirizacao.model;

public class MenorTempo extends CriterioRotaBase {

    @Override
    protected double pesoDe(Via via) {
        return via.getTempoMin();
    }

    @Override
    public String descricao() {
        return "menor tempo (min)";
    }
}
