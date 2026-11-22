package com.logistica.roteirizacao.model;

/** Contrato que define o peso de uma via no cálculo de rota. */
public interface CriterioRota {

    /**
     * Peso da via no Dijkstra.
     *
     * @param via via avaliada
     * @return peso positivo
     */
    double peso(Via via);

    /**
     * Texto do critério para mensagens.
     *
     * @return descrição curta, ex.: "menor distância (km)"
     */
    String descricao();
}
