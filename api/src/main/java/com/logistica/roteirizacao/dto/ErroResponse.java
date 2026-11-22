package com.logistica.roteirizacao.dto;

import com.logistica.roteirizacao.exception.TipoErro;

/**
 * Formato único de erro da API (schema "Erro" do openapi.yaml).
 *
 * @param status   código HTTP
 * @param erro     tipo do erro (nome de {@link TipoErro})
 * @param campo    campo que causou o erro, ou {@code null}
 * @param mensagem explicação para o usuário, dizendo como corrigir
 */
public record ErroResponse(int status, String erro, String campo, String mensagem) {

    /**
     * Monta o erro a partir do tipo, que define o status HTTP.
     *
     * @param tipo     tipo do erro
     * @param campo    campo que causou o erro, ou {@code null}
     * @param mensagem explicação para o usuário
     * @return o corpo de erro pronto para a resposta
     */
    public static ErroResponse de(TipoErro tipo, String campo, String mensagem) {
        return new ErroResponse(tipo.getStatus(), tipo.name(), campo, mensagem);
    }
}
