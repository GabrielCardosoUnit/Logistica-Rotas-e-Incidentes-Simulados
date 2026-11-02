package com.logistica.roteirizacao.client.dto;

import java.time.OffsetDateTime;

/** Resposta de POST /incidentes (schema IncidenteResponse do openapi.yaml). */
public record IncidenteResultado(
        long id,
        String viaId,
        String descricao,
        boolean ativo,
        OffsetDateTime registradoEm,
        int versaoRede) {
}
