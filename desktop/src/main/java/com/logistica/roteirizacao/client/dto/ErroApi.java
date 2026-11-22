package com.logistica.roteirizacao.client.dto;

/**
 * Corpo de erro padrão da API (schema Erro do openapi.yaml).
 *
 * @param status   código HTTP
 * @param erro     tipo do erro, ex.: {@code VALIDACAO}
 * @param campo    campo que causou o erro, quando houver (a tela pode destacá-lo)
 * @param mensagem explicação pronta para o usuário
 */
public record ErroApi(int status, String erro, String campo, String mensagem) {
}
