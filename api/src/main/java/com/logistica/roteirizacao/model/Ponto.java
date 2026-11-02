package com.logistica.roteirizacao.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Vértice da rede. Latitude e longitude servem só para desenhar o mapa;
 * a conectividade vem exclusivamente das vias.
 */
@Entity
@Table(name = "ponto")
public class Ponto {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected Ponto() {
    }

    public Ponto(String id, String nome, double latitude, double longitude) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id do ponto é obrigatório.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do ponto é obrigatório.");
        }
        if (!(latitude >= -90 && latitude <= 90)) {
            throw new IllegalArgumentException("latitude deve estar entre -90 e 90 (valor: " + latitude + ").");
        }
        if (!(longitude >= -180 && longitude <= 180)) {
            throw new IllegalArgumentException("longitude deve estar entre -180 e 180 (valor: " + longitude + ").");
        }
        this.id = id.strip();
        this.nome = nome.strip();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** Dois pontos são o mesmo ponto quando têm o mesmo id. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ponto outro)) {
            return false;
        }
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return id + " (" + nome + ")";
    }
}
