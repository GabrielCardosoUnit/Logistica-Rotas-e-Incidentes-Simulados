package com.logistica.roteirizacao.client.dto;

import java.util.List;

/**
 * Resposta de POST /redes/importacoes (schema ImportacaoResponse do openapi.yaml).
 *
 * @param versaoRede       versão da rede gerada pela importação
 * @param pontosImportados quantidade de pontos aceitos
 * @param viasImportadas   quantidade de vias aceitas
 * @param linhasRejeitadas linhas descartadas, com o motivo
 */
public record ImportacaoResultado(
        int versaoRede,
        int pontosImportados,
        int viasImportadas,
        List<LinhaRejeitada> linhasRejeitadas) {

    /**
     * Troca {@code null} por lista vazia e guarda uma cópia imutável.
     *
     * @param versaoRede       versão da rede
     * @param pontosImportados quantidade de pontos aceitos
     * @param viasImportadas   quantidade de vias aceitas
     * @param linhasRejeitadas linhas descartadas
     */
    public ImportacaoResultado {
        linhasRejeitadas = linhasRejeitadas == null ? List.of() : List.copyOf(linhasRejeitadas);
    }

    /**
     * Diz se alguma linha foi descartada.
     *
     * @return {@code true} se houve linhas rejeitadas
     */
    public boolean teveRejeicoes() {
        return !linhasRejeitadas.isEmpty();
    }

    /**
     * Uma linha de CSV recusada pela API, com o motivo.
     *
     * @param arquivo nome do arquivo (pontos.csv ou vias.csv)
     * @param linha   número da linha, contando o cabeçalho como 1
     * @param motivo  por que a linha foi rejeitada
     */
    public record LinhaRejeitada(String arquivo, int linha, String motivo) {

        @Override
        public String toString() {
            return arquivo + ", linha " + linha + ": " + motivo;
        }
    }
}
