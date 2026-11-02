package com.logistica.roteirizacao.client.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta de POST /rotas e GET /rotas/{id} (schema RotaResponse do openapi.yaml).
 *
 * <p>Os totais são {@code Double} porque chegam {@code null} quando não há caminho.
 */
public record RotaResultado(
        long id,
        String origemId,
        String destinoId,
        Criterio criterio,
        String status,
        List<String> pontos,
        List<String> vias,
        Double distanciaTotalKm,
        Double tempoTotalMin,
        int versaoRede,
        String mensagem,
        OffsetDateTime calculadaEm) {

    public static final String STATUS_SEM_CAMINHO = "SEM_CAMINHO";

    public RotaResultado {
        pontos = pontos == null ? List.of() : List.copyOf(pontos);
        vias = vias == null ? List.of() : List.copyOf(vias);
    }

    /** "Sem caminho" não é erro da API: chega como 201 com este status. */
    public boolean semCaminho() {
        return STATUS_SEM_CAMINHO.equals(status);
    }
}
