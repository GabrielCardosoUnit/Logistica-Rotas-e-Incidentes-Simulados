package com.logistica.roteirizacao.dto;

import java.util.List;

public record ImportacaoResponse(
        int versaoRede,
        int pontosImportados,
        int viasImportadas,
        List<LinhaRejeitadaResponse> linhasRejeitadas) {
}
