package com.logistica.roteirizacao.dto;

import com.logistica.roteirizacao.files.LinhaRejeitada;

public record LinhaRejeitadaResponse(String arquivo, int linha, String motivo) {

    public static LinhaRejeitadaResponse de(LinhaRejeitada linha) {
        return new LinhaRejeitadaResponse(linha.arquivo(), linha.linha(), linha.motivo());
    }
}
