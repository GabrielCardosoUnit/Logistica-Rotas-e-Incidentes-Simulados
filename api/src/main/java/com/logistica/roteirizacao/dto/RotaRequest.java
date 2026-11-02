package com.logistica.roteirizacao.dto;

import com.logistica.roteirizacao.model.TipoCriterio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RotaRequest(
        @NotBlank(message = "Informe o ponto de origem.")
        String origemId,

        @NotBlank(message = "Informe o ponto de destino.")
        String destinoId,

        @NotNull(message = "Informe o critério: MENOR_DISTANCIA ou MENOR_TEMPO.")
        TipoCriterio criterio) {
}
