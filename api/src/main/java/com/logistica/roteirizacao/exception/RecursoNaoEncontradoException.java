package com.logistica.roteirizacao.exception;

public class RecursoNaoEncontradoException extends ApiException {

    public RecursoNaoEncontradoException(String campo, String mensagem) {
        super(TipoErro.NAO_ENCONTRADO, campo, mensagem);
    }
}
