package com.logistica.roteirizacao.exception;

/**
 * Base das exceções que viram resposta de erro da API. Carrega o tipo
 * (que define o status HTTP) e o campo que causou o problema, quando houver.
 * Quem traduz para JSON é o {@link TratadorErrosApi}.
 */
public abstract class ApiException extends RuntimeException {

    private final TipoErro tipo;
    private final String campo;

    protected ApiException(TipoErro tipo, String campo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.campo = campo;
    }

    public TipoErro getTipo() {
        return tipo;
    }

    public String getCampo() {
        return campo;
    }
}
