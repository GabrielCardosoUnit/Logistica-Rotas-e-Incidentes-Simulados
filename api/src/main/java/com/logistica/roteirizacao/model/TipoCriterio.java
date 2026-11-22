package com.logistica.roteirizacao.model;

/**
 * Valor do campo "criterio" na API. Cada constante sabe qual estratégia de
 * peso ({@link CriterioRota}) usar no Dijkstra.
 */
public enum TipoCriterio {
    /** Peso = distância da via (km). */
    MENOR_DISTANCIA {
        @Override
        public CriterioRota estrategia() {
            return new MenorDistancia();
        }
    },
    /** Peso = tempo de percurso da via (min). */
    MENOR_TEMPO {
        @Override
        public CriterioRota estrategia() {
            return new MenorTempo();
        }
    };

    /**
     * Estratégia de peso deste critério.
     *
     * @return nova instância do critério
     */
    public abstract CriterioRota estrategia();
}
