package com.logistica.roteirizacao.dto;

import java.util.List;

/**
 * Resultado da importação da rede.
 *
 * @param versaoRede       versão da rede gerada pela importação
 * @param pontosImportados quantidade de pontos aceitos
 * @param viasImportadas   quantidade de vias aceitas
 * @param linhasRejeitadas linhas dos CSVs que foram descartadas, com o motivo
 */
public record ImportacaoResponse(
        int versaoRede,
        int pontosImportados,
        int viasImportadas,
        List<LinhaRejeitadaResponse> linhasRejeitadas) {
}
