package com.logistica.roteirizacao.client.dto;

/**
 * Corpo de erro padrão da API (schema Erro do openapi.yaml).
 *
 * @param campo campo que causou o erro, quando houver (a tela pode destacá-lo)
 */
public record ErroApi(int status, String erro, String campo, String mensagem) {
}
