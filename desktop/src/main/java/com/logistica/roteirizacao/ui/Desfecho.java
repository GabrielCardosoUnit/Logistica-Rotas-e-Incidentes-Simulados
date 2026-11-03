package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.ClienteApiException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Resultado de uma chamada à API: ou deu certo (valor) ou a API/cliente recusou (ClienteApiException).
 *
 * <p>Por que existe: {@code TarefaEmSegundoPlano} só entrega o TEXTO do erro. Para a tela destacar
 * o campo certo (origemId, destinoId, pontos, vias), precisamos da exceção inteira. Então a
 * chamada captura a ClienteApiException aqui, dentro da thread de segundo plano, e a devolve
 * como valor. Qualquer outro erro continua indo para o aoFalhar da tarefa.
 */
public final class Desfecho<T> {

    private final T valor;
    private final ClienteApiException falha;

    private Desfecho(T valor, ClienteApiException falha) {
        this.valor = valor;
        this.falha = falha;
    }

    /** Executa a chamada e transforma uma ClienteApiException em valor. */
    public static <T> Desfecho<T> de(Callable<T> chamada) throws Exception {
        try {
            return new Desfecho<>(chamada.call(), null);
        } catch (ClienteApiException e) {
            return new Desfecho<>(null, e);
        }
    }

    /** Chame na EDT: roda um dos dois consumidores, conforme o resultado. */
    public void tratar(Consumer<T> aoDarCerto, Consumer<ClienteApiException> aoFalhar) {
        if (falha != null) {
            aoFalhar.accept(falha);
        } else {
            aoDarCerto.accept(valor);
        }
    }
}
