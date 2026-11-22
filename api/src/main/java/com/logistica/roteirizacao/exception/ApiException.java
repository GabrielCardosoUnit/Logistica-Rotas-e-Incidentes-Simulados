package com.logistica.roteirizacao.exception;

/**
 * Base das exceções que viram resposta de erro da API. Carrega o tipo
 * (que define o status HTTP) e o campo que causou o problema, quando houver.
 * Quem traduz para JSON é o {@link TratadorErrosApi}.
 */
public abstract class ApiException extends RuntimeException {

    /** Tipo do erro, que define o status HTTP. */
    private final TipoErro tipo;
    /** Campo que causou o erro, ou {@code null}. */
    private final String campo;

    /**
     * Cria a exceção.
     *
     * @param tipo     tipo do erro
     * @param campo    campo que causou o erro, ou {@code null}
     * @param mensagem explicação para o usuário
     */
    protected ApiException(TipoErro tipo, String campo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.campo = campo;
    }

    /**
     * Tipo do erro.
     *
     * @return o tipo, que define o status HTTP
     */
    public TipoErro getTipo() {
        return tipo;
    }

    /**
     * Campo que causou o erro.
     *
     * @return nome do campo, ou {@code null} se o erro não é de um campo
     */
    public String getCampo() {
        return campo;
    }
}
