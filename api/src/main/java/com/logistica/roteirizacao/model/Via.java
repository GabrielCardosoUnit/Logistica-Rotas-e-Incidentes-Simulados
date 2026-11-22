package com.logistica.roteirizacao.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Trecho entre dois pontos. Decisão da equipe (docs/openapi.yaml e
 * docs/dicionario-dados.md): cada via é de mão dupla e um incidente bloqueia
 * os dois sentidos. O {@link Grafo} cria as duas direções.
 */
@Entity
@Table(name = "via")
public class Via {

    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_id", nullable = false)
    private Ponto origem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private Ponto destino;

    @Column(name = "distancia_km", nullable = false)
    private double distanciaKm;

    @Column(name = "tempo_min", nullable = false)
    private double tempoMin;

    @Column(nullable = false)
    private boolean habilitada;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected Via() {
    }

    /**
     * Cria uma via, validando os dados.
     *
     * @param id          identificador único
     * @param origem      ponto de uma ponta
     * @param destino     ponto da outra ponta (diferente da origem)
     * @param distanciaKm distância, maior que zero
     * @param tempoMin    tempo de percurso, maior que zero
     * @param habilitada  {@code false} se a via vem fechada no CSV
     * @throws IllegalArgumentException se algum valor for inválido
     * @throws NullPointerException se origem ou destino forem nulos
     */
    public Via(String id, Ponto origem, Ponto destino, double distanciaKm, double tempoMin, boolean habilitada) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id da via é obrigatório.");
        }
        Objects.requireNonNull(origem, "origem da via é obrigatória.");
        Objects.requireNonNull(destino, "destino da via é obrigatório.");
        if (origem.equals(destino)) {
            throw new IllegalArgumentException("origem e destino da via devem ser diferentes (" + origem.getId() + ").");
        }
        // Dijkstra só funciona com pesos não negativos; zero também é recusado
        // porque não representa um trecho real.
        validarPeso("distanciaKm", distanciaKm);
        validarPeso("tempoMin", tempoMin);
        this.id = id.strip();
        this.origem = origem;
        this.destino = destino;
        this.distanciaKm = distanciaKm;
        this.tempoMin = tempoMin;
        this.habilitada = habilitada;
    }

    private static void validarPeso(String campo, double valor) {
        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero (valor: " + valor + ").");
        }
    }

    /**
     * Identificador da via.
     *
     * @return o id
     */
    public String getId() {
        return id;
    }

    /**
     * Ponto de uma ponta da via.
     *
     * @return a origem
     */
    public Ponto getOrigem() {
        return origem;
    }

    /**
     * Ponto da outra ponta da via.
     *
     * @return o destino
     */
    public Ponto getDestino() {
        return destino;
    }

    /**
     * Distância da via.
     *
     * @return distância em km
     */
    public double getDistanciaKm() {
        return distanciaKm;
    }

    /**
     * Tempo de percurso da via.
     *
     * @return tempo em minutos
     */
    public double getTempoMin() {
        return tempoMin;
    }

    /**
     * Diz se a via estava aberta na importação.
     *
     * @return {@code false} se a via veio desabilitada no CSV
     */
    public boolean isHabilitada() {
        return habilitada;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Via outra)) {
            return false;
        }
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return id + " " + origem.getId() + "<->" + destino.getId()
                + " (" + distanciaKm + " km, " + tempoMin + " min" + (habilitada ? "" : ", desabilitada") + ")";
    }
}
