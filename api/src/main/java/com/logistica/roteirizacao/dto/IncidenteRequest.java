package com.logistica.roteirizacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /incidentes}: bloqueia uma via.
 *
 * @param viaId     identificador da via a ser bloqueada
 * @param descricao motivo do bloqueio (3 a 200 caracteres)
 */
public record IncidenteRequest(
        @NotBlank(message = "Informe a via a ser bloqueada.")
        String viaId,

        @NotBlank(message = "Descreva o incidente.")
        @Size(min = 3, max = 200, message = "A descrição deve ter entre 3 e 200 caracteres.")
        String descricao) {
}
