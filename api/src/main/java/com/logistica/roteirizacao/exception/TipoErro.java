package com.logistica.roteirizacao.exception;

/** Valores do campo "erro" na resposta de erro, com o status HTTP de cada um. */
public enum TipoErro {
    /** Dado de entrada inválido (400). */
    VALIDACAO(400),
    /** Recurso inexistente (404). */
    NAO_ENCONTRADO(404),
    /** Operação conflita com o estado atual (409). */
    CONFLITO(409),
    /** Banco de dados fora do ar (503). */
    INDISPONIVEL(503),
    /** Falha inesperada no servidor (500). */
    INTERNO(500);

    private final int status;

    TipoErro(int status) {
        this.status = status;
    }

    /**
     * Status HTTP deste tipo de erro.
     *
     * @return o código HTTP
     */
    public int getStatus() {
        return status;
    }
}
