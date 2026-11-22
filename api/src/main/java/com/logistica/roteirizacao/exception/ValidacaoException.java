package com.logistica.roteirizacao.exception;

/** Dado de entrada inválido (HTTP 400). */
public class ValidacaoException extends ApiException {

    /**
     * Cria a exceção.
     *
     * @param campo    campo inválido, ou {@code null}
     * @param mensagem explicação para o usuário
     */
    public ValidacaoException(String campo, String mensagem) {
        super(TipoErro.VALIDACAO, campo, mensagem);
    }
}
