package com.logistica.roteirizacao.model;

/** Resultado de um cálculo de rota. */
public enum StatusRota {
    /** Caminho encontrado. */
    CALCULADA,
    /** Destino não alcançável com as vias disponíveis. */
    SEM_CAMINHO
}
