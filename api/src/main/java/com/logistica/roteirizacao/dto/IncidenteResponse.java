package com.logistica.roteirizacao.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.logistica.roteirizacao.model.Incidente;

public record IncidenteResponse(
        Long id,
        String viaId,
        String descricao,
        boolean ativo,
        OffsetDateTime registradoEm,
        int versaoRede) {

    public static IncidenteResponse de(Incidente incidente, int versaoRede, ZoneId fuso) {
        return new IncidenteResponse(
                incidente.getId(),
                incidente.getVia().getId(),
                incidente.getDescricao(),
                incidente.isAtivo(),
                OffsetDateTime.ofInstant(incidente.getRegistradoEm(), fuso),
                versaoRede);
    }
}
