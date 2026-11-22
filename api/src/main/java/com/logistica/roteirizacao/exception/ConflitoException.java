package com.logistica.roteirizacao.exception;

/** Operação conflita com o estado atual (HTTP 409), ex.: via que já está bloqueada. */
public class ConflitoException extends ApiException {

    /**
     * Cria a exceção.
     *
     * @param campo    campo em conflito
     * @param mensagem explicação para o usuário
     */
    public ConflitoException(String campo, String mensagem) {
        super(TipoErro.CONFLITO, campo, mensagem);
    }
}
