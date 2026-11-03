package com.logistica.roteirizacao.ui;

/** Via da rede, como a GUI a enxerga (vem do vias.csv). Serve só para desenhar o mapa. */
public final class ViaRede {

    private final String id;
    private final String origemId;
    private final String destinoId;
    private final boolean habilitada;

    public ViaRede(String id, String origemId, String destinoId, boolean habilitada) {
        if (id == null || id.isBlank() || origemId == null || origemId.isBlank()
                || destinoId == null || destinoId.isBlank()) {
            throw new IllegalArgumentException("id, origem e destino da via são obrigatórios");
        }
        this.id = id.trim();
        this.origemId = origemId.trim();
        this.destinoId = destinoId.trim();
        this.habilitada = habilitada;
    }

    public String getId() { return id; }
    public String getOrigemId() { return origemId; }
    public String getDestinoId() { return destinoId; }
    public boolean isHabilitada() { return habilitada; }

    @Override
    public String toString() {
        return id + " (" + origemId + " — " + destinoId + ")";
    }
}
