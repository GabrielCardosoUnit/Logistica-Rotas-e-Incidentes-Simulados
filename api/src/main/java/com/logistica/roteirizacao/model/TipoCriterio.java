package com.logistica.roteirizacao.model;

/**
 * Valor do campo "criterio" na API. Cada constante sabe qual estratégia de
 * peso ({@link CriterioRota}) usar no Dijkstra.
 */
public enum TipoCriterio {
    MENOR_DISTANCIA {
        @Override
        public CriterioRota estrategia() {
            return new MenorDistancia();
        }
    },
    MENOR_TEMPO {
        @Override
        public CriterioRota estrategia() {
            return new MenorTempo();
        }
    };

    public abstract CriterioRota estrategia();
}
