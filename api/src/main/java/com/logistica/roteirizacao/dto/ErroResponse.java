package com.logistica.roteirizacao.dto;

import com.logistica.roteirizacao.exception.TipoErro;

/** Formato único de erro da API (schema "Erro" do openapi.yaml). */
public record ErroResponse(int status, String erro, String campo, String mensagem) {

    public static ErroResponse de(TipoErro tipo, String campo, String mensagem) {
        return new ErroResponse(tipo.getStatus(), tipo.name(), campo, mensagem);
    }
}
