package com.logistica.roteirizacao.model;

/** Contrato que define o peso de uma via no cálculo de rota. */
public interface CriterioRota {

    double peso(Via via);

    String descricao();
}
