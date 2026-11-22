package com.logistica.roteirizacao.model;

/** Critério que usa a distância da via (km) como peso. */
public class MenorDistancia extends CriterioRotaBase {

    /** Cria o critério. */
    public MenorDistancia() {
    }

    @Override
    protected double pesoDe(Via via) {
        return via.getDistanciaKm();
    }

    @Override
    public String descricao() {
        return "menor distância (km)";
    }
}
