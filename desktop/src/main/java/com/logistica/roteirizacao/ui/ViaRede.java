package com.logistica.roteirizacao.ui;

/** Via da rede, como a GUI a enxerga (vem do vias.csv). Serve só para desenhar o mapa. */
public final class ViaRede {

    private final String id;
    private final String origemId;
    private final String destinoId;
    private final boolean habilitada;

    /**
     * Cria a via.
     *
     * @param id         identificador
     * @param origemId   ponto de uma ponta
     * @param destinoId  ponto da outra ponta
     * @param habilitada {@code false} se a via vem fechada no CSV
     */
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

    /**
     * Identificador da via.
     *
     * @return o id
     */
    public String getId() { return id; }
    /**
     * Ponto de uma ponta.
     *
     * @return id da origem
     */
    public String getOrigemId() { return origemId; }
    /**
     * Ponto da outra ponta.
     *
     * @return id do destino
     */
    public String getDestinoId() { return destinoId; }
    /**
     * Diz se a via estava aberta no CSV.
     *
     * @return {@code false} se veio desabilitada
     */
    public boolean isHabilitada() { return habilitada; }

    @Override
    public String toString() {
        return id + " (" + origemId + " — " + destinoId + ")";
    }
}
