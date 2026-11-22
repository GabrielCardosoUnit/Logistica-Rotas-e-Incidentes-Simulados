package com.logistica.roteirizacao.exception;

/** Ponto, via, rota ou incidente inexistente (HTTP 404). */
public class RecursoNaoEncontradoException extends ApiException {

    /**
     * Cria a exceção.
     *
     * @param campo    campo com o identificador não encontrado
     * @param mensagem explicação para o usuário
     */
    public RecursoNaoEncontradoException(String campo, String mensagem) {
        super(TipoErro.NAO_ENCONTRADO, campo, mensagem);
    }
}
