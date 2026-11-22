package com.logistica.roteirizacao.files;

import java.io.IOException;

/** O config.bin não pôde ser usado (ausente, truncado, versão ou valor inválido). */
public class ConfiguracaoInvalidaException extends IOException {

    /**
     * Cria a exceção.
     *
     * @param mensagem texto para o usuário
     */
    public ConfiguracaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    /**
     * Cria a exceção a partir de outra falha.
     *
     * @param mensagem texto para o usuário
     * @param causa    falha original
     */
    public ConfiguracaoInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
