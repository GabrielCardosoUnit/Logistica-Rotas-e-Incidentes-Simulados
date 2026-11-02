package com.logistica.roteirizacao.model;

import java.util.Objects;

/**
 * Validação comum às estratégias: a via não pode ser nula e o peso devolvido
 * precisa ser positivo, senão o Dijkstra deixa de ser correto.
 */
public abstract class CriterioRotaBase implements CriterioRota {

    @Override
    public final double peso(Via via) {
        Objects.requireNonNull(via, "via é obrigatória para calcular o peso.");
        double peso = pesoDe(via);
        if (!Double.isFinite(peso) || peso <= 0) {
            throw new IllegalStateException("Peso inválido para a via " + via.getId() + ": " + peso);
        }
        return peso;
    }

    protected abstract double pesoDe(Via via);

    @Override
    public String toString() {
        return descricao();
    }
}
