package com.logistica.roteirizacao.client.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta de POST /rotas e GET /rotas/{id} (schema RotaResponse do openapi.yaml).
 *
 * <p>Os totais são {@code Double} porque chegam {@code null} quando não há caminho.
 *
 * @param id               identificador da rota
 * @param origemId         ponto de partida
 * @param destinoId        ponto de chegada
 * @param criterio         critério usado
 * @param status           {@code CALCULADA} ou {@value #STATUS_SEM_CAMINHO}
 * @param pontos           pontos percorridos, em ordem
 * @param vias             vias percorridas, em ordem
 * @param distanciaTotalKm distância total, ou {@code null} sem caminho
 * @param tempoTotalMin    tempo total, ou {@code null} sem caminho
 * @param versaoRede       versão da rede usada no cálculo
 * @param mensagem         explicação para o usuário, quando houver
 * @param calculadaEm      momento do cálculo
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

    /**
     * Troca listas {@code null} por vazias e guarda cópias imutáveis.
     *
     * @param id               identificador da rota
     * @param origemId         ponto de partida
     * @param destinoId        ponto de chegada
     * @param criterio         critério usado
     * @param status           status do cálculo
     * @param pontos           pontos percorridos
     * @param vias             vias percorridas
     * @param distanciaTotalKm distância total
     * @param tempoTotalMin    tempo total
     * @param versaoRede       versão da rede
     * @param mensagem         explicação para o usuário
     * @param calculadaEm      momento do cálculo
     */
    public RotaResultado {
        pontos = pontos == null ? List.of() : List.copyOf(pontos);
        vias = vias == null ? List.of() : List.copyOf(vias);
    }

    /**
     * "Sem caminho" não é erro da API: chega como 201 com este status.
     *
     * @return {@code true} se o destino não era alcançável
     */
    public boolean semCaminho() {
        return STATUS_SEM_CAMINHO.equals(status);
    }
}
