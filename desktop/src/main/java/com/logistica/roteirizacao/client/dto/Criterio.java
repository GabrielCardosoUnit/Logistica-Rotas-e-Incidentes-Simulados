package com.logistica.roteirizacao.client.dto;

/** Critérios aceitos pela API em POST /rotas. */
public enum Criterio {
    MENOR_DISTANCIA("Menor distância (km)"),
    MENOR_TEMPO("Menor tempo (min)");

    private final String descricao;

    Criterio(String descricao) {
        this.descricao = descricao;
    }

    /** Texto para exibir na tela. */
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
