package com.logistica.roteirizacao.client.dto;

import java.time.OffsetDateTime;

/**
 * Resposta de POST /incidentes e de POST /incidentes/{id}/encerramento
 * (schema IncidenteResponse do openapi.yaml). O campo encerradoEm da API é ignorado.
 *
 * @param id           identificador do incidente
 * @param viaId        via bloqueada
 * @param descricao    motivo do bloqueio
 * @param ativo        {@code true} enquanto a via continua bloqueada
 * @param registradoEm quando o incidente foi registrado
 * @param versaoRede   versão da rede após a operação
 */
public record IncidenteResultado(
        long id,
        String viaId,
        String descricao,
        boolean ativo,
        OffsetDateTime registradoEm,
        int versaoRede) {
}
