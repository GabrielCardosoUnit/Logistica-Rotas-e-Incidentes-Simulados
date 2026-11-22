package com.logistica.roteirizacao.dto;

import com.logistica.roteirizacao.model.TipoCriterio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /rotas}.
 *
 * @param origemId  ponto de partida
 * @param destinoId ponto de chegada
 * @param criterio  critério de custo usado no Dijkstra
 */
public record RotaRequest(
        @NotBlank(message = "Informe o ponto de origem.")
        String origemId,

        @NotBlank(message = "Informe o ponto de destino.")
        String destinoId,

        @NotNull(message = "Informe o critério: MENOR_DISTANCIA ou MENOR_TEMPO.")
        TipoCriterio criterio) {
}
