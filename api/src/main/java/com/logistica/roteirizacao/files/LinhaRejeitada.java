package com.logistica.roteirizacao.files;

/**
 * Linha de CSV que não foi importada. "linha" conta o cabeçalho como 1.
 *
 * @param arquivo nome do arquivo (pontos.csv ou vias.csv)
 * @param linha   número da linha no arquivo
 * @param motivo  por que a linha foi rejeitada
 */
public record LinhaRejeitada(String arquivo, int linha, String motivo) {
}
