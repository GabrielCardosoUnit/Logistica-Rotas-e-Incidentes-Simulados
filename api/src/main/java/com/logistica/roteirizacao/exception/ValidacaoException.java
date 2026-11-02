package com.logistica.roteirizacao.exception;

public class ValidacaoException extends ApiException {

    public ValidacaoException(String campo, String mensagem) {
        super(TipoErro.VALIDACAO, campo, mensagem);
    }
}
