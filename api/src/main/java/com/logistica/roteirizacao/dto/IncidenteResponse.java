package com.logistica.roteirizacao.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.logistica.roteirizacao.model.Incidente;

/**
 * Incidente devolvido pela API.
 *
 * @param id           identificador do incidente
 * @param viaId        via bloqueada
 * @param descricao    motivo do bloqueio
 * @param ativo        {@code true} enquanto a via continua bloqueada
 * @param registradoEm quando o incidente foi registrado
 * @param encerradoEm  quando foi encerrado, ou {@code null} se ainda ativo
 * @param versaoRede   versão da rede após a operação
 */
public record IncidenteResponse(
        Long id,
        String viaId,
        String descricao,
        boolean ativo,
        OffsetDateTime registradoEm,
        OffsetDateTime encerradoEm,
        int versaoRede) {

    /**
     * Converte a entidade para a resposta da API.
     *
     * @param incidente  incidente gravado
     * @param versaoRede versão da rede após a operação
     * @param fuso       fuso usado para exibir os instantes
     * @return a resposta
     */
    public static IncidenteResponse de(Incidente incidente, int versaoRede, ZoneId fuso) {
        return new IncidenteResponse(
                incidente.getId(),
                incidente.getVia().getId(),
                incidente.getDescricao(),
                incidente.isAtivo(),
                OffsetDateTime.ofInstant(incidente.getRegistradoEm(), fuso),
                incidente.getEncerradoEm() == null ? null : OffsetDateTime.ofInstant(incidente.getEncerradoEm(), fuso),
                versaoRede);
    }
}
