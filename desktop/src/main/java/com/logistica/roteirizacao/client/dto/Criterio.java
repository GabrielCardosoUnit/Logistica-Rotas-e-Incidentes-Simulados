package com.logistica.roteirizacao.client.dto;

/** Critérios aceitos pela API em POST /rotas. */
public enum Criterio {
    /** Peso = distância da via (km). */
    MENOR_DISTANCIA("Menor distância (km)"),
    /** Peso = tempo de percurso da via (min). */
    MENOR_TEMPO("Menor tempo (min)");

    private final String descricao;

    Criterio(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Texto para exibir na tela.
     *
     * @return descrição do critério, com a unidade
     */
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
