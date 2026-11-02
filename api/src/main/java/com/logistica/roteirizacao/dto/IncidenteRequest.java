package com.logistica.roteirizacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidenteRequest(
        @NotBlank(message = "Informe a via a ser bloqueada.")
        String viaId,

        @NotBlank(message = "Descreva o incidente.")
        @Size(min = 3, max = 200, message = "A descrição deve ter entre 3 e 200 caracteres.")
        String descricao) {
}
