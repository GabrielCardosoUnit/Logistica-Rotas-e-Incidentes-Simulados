package com.logistica.roteirizacao.files;

/** Linha de CSV que não foi importada. "linha" conta o cabeçalho como 1. */
public record LinhaRejeitada(String arquivo, int linha, String motivo) {
}
