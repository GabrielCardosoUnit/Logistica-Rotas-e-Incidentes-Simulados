package com.logistica.roteirizacao.files;

import java.io.IOException;

/** O config.bin não pôde ser usado (ausente, truncado, versão ou valor inválido). */
public class ConfiguracaoInvalidaException extends IOException {

    public ConfiguracaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    public ConfiguracaoInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
