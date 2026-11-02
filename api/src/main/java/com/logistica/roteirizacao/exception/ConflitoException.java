package com.logistica.roteirizacao.exception;

public class ConflitoException extends ApiException {

    public ConflitoException(String campo, String mensagem) {
        super(TipoErro.CONFLITO, campo, mensagem);
    }
}
