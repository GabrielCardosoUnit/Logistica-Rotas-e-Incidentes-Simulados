package com.logistica.roteirizacao.client.dto;

import java.util.List;

/** Resposta de POST /redes/importacoes (schema ImportacaoResponse do openapi.yaml). */
public record ImportacaoResultado(
        int versaoRede,
        int pontosImportados,
        int viasImportadas,
        List<LinhaRejeitada> linhasRejeitadas) {

    public ImportacaoResultado {
        linhasRejeitadas = linhasRejeitadas == null ? List.of() : List.copyOf(linhasRejeitadas);
    }

    public boolean teveRejeicoes() {
        return !linhasRejeitadas.isEmpty();
    }

    /** Uma linha de CSV recusada pela API, com o motivo. */
    public record LinhaRejeitada(String arquivo, int linha, String motivo) {

        @Override
        public String toString() {
            return arquivo + ", linha " + linha + ": " + motivo;
        }
    }
}
