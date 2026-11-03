package com.logistica.roteirizacao.ui;

import java.util.Objects;

/**
 * Ponto da rede, como a GUI o enxerga (vem do pontos.csv escolhido na importação).
 * Serve de item dos combos e guardará latitude/longitude para o mapa esquemático.
 */
public final class PontoRede {

    private final String id;
    private final String nome;
    private final double latitude;
    private final double longitude;

    public PontoRede(String id, String nome, double latitude, double longitude) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id do ponto é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do ponto é obrigatório");
        }
        this.id = id.trim();
        this.nome = nome.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    /** Igualdade só pelo id, como o domínio da API. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PontoRede)) {
            return false;
        }
        return id.equals(((PontoRede) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Texto mostrado nos combos e na lista de passos. */
    @Override
    public String toString() {
        return id + " — " + nome;
    }
}
