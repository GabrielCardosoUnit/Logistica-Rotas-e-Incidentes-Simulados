package com.logistica.roteirizacao.model;

public class MenorDistancia extends CriterioRotaBase {

    @Override
    protected double pesoDe(Via via) {
        return via.getDistanciaKm();
    }

    @Override
    public String descricao() {
        return "menor distância (km)";
    }
}
