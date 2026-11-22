package com.logistica.roteirizacao.dto;

import com.logistica.roteirizacao.files.LinhaRejeitada;

/**
 * Linha de CSV descartada na importação.
 *
 * @param arquivo nome do arquivo (pontos ou vias)
 * @param linha   número da linha no arquivo
 * @param motivo  por que a linha foi rejeitada
 */
public record LinhaRejeitadaResponse(String arquivo, int linha, String motivo) {

    /**
     * Converte a linha rejeitada do importador para a resposta da API.
     *
     * @param linha linha rejeitada pelo importador
     * @return a resposta
     */
    public static LinhaRejeitadaResponse de(LinhaRejeitada linha) {
        return new LinhaRejeitadaResponse(linha.arquivo(), linha.linha(), linha.motivo());
    }
}
