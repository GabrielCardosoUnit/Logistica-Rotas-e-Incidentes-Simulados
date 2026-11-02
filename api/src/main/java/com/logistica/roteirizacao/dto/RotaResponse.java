package com.logistica.roteirizacao.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.logistica.roteirizacao.model.RotaCalculada;
import com.logistica.roteirizacao.model.StatusRota;
import com.logistica.roteirizacao.model.TipoCriterio;

public record RotaResponse(
        Long id,
        String origemId,
        String destinoId,
        TipoCriterio criterio,
        StatusRota status,
        List<String> pontos,
        List<String> vias,
        Double distanciaTotalKm,
        Double tempoTotalMin,
        int versaoRede,
        String mensagem,
        OffsetDateTime calculadaEm) {

    public static RotaResponse de(RotaCalculada rota, ZoneId fuso) {
        return new RotaResponse(
                rota.getId(),
                rota.getOrigemId(),
                rota.getDestinoId(),
                rota.getCriterio(),
                rota.getStatus(),
                rota.getPontos(),
                rota.getVias(),
                rota.getDistanciaTotalKm(),
                rota.getTempoTotalMin(),
                rota.getVersaoRede(),
                rota.getMensagem(),
                OffsetDateTime.ofInstant(rota.getCalculadaEm(), fuso));
    }
}
