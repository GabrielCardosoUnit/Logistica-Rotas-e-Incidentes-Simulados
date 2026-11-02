package com.logistica.roteirizacao.exception;

/** Valores do campo "erro" na resposta de erro, com o status HTTP de cada um. */
public enum TipoErro {
    VALIDACAO(400),
    NAO_ENCONTRADO(404),
    CONFLITO(409),
    INDISPONIVEL(503),
    INTERNO(500);

    private final int status;

    TipoErro(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
