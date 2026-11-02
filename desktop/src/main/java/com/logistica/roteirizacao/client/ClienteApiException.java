package com.logistica.roteirizacao.client;

import java.util.Optional;

import com.logistica.roteirizacao.client.dto.ErroApi;

/**
 * Falha ao falar com a API. {@link #getMessage()} já está escrita para o usuário
 * final, então a tela pode exibi-la diretamente.
 */
public class ClienteApiException extends Exception {

    private final transient ErroApi erroApi;

    /** Falha sem resposta da API (conexão recusada, timeout, arquivo local inválido). */
    public ClienteApiException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.erroApi = null;
    }

    public ClienteApiException(String mensagem) {
        this(mensagem, null);
    }

    /** A API respondeu com erro no formato padrão (400, 404, 409, 503). */
    public ClienteApiException(ErroApi erroApi) {
        super(erroApi.mensagem());
        this.erroApi = erroApi;
    }

    /** Corpo de erro da API, quando ela chegou a responder. */
    public Optional<ErroApi> getErroApi() {
        return Optional.ofNullable(erroApi);
    }

    /** Campo apontado pela API como causa (ex.: "origemId"), para a tela destacar. */
    public Optional<String> getCampo() {
        return getErroApi().map(ErroApi::campo);
    }

    /** Status HTTP da resposta de erro, ou vazio se a API não respondeu. */
    public Optional<Integer> getStatusHttp() {
        return getErroApi().map(ErroApi::status);
    }
}
